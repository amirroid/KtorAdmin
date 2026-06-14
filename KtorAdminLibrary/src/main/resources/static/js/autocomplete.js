class AutoComplete {
    constructor(inputElement, config) {
        this.input = inputElement;
        this.config = {
            tableName: config.tableName,
            columnName: config.columnName,
            debounceMs: config.debounceMs || 300,
            adminPath: config.adminPath || 'admin',
        };
        this.selectedKey = null;
        this.selectedLabel = '';
        this.isOpen = false;
        this.isLoading = false;
        this.searchTimeout = null;
        this.currentPage = 0;
        this.hasMore = true;
        this.items = [];
        this.highlightedIndex = -1;

        this.hiddenInput = this.input.parentNode.querySelector('input[type="hidden"]');
        this.wrapper = null;
        this.dropdown = null;
        this.searchInput = null;
        this.listElement = null;
        this.loadingElement = null;

        this.init();
    }

    init() {
        this.createDropdown();
        this.bindEvents();
        this.loadInitialValue();
    }

    createDropdown() {
        this.wrapper = document.createElement('div');
        this.wrapper.className = 'autocomplete-wrapper';

        this.trigger = document.createElement('div');
        this.trigger.className = 'autocomplete-trigger';
        this.trigger.setAttribute('tabindex', '0');
        this.trigger.setAttribute('role', 'combobox');
        this.trigger.setAttribute('aria-expanded', 'false');
        this.trigger.setAttribute('aria-haspopup', 'listbox');

        this.selectedDisplay = document.createElement('span');
        this.selectedDisplay.className = 'autocomplete-selected';
        this.selectedDisplay.textContent = this.input.value || '---';

        this.clearButton = document.createElement('span');
        this.clearButton.className = 'autocomplete-clear';
        this.clearButton.innerHTML = '&times;';
        this.clearButton.style.display = 'none';

        this.arrowIcon = document.createElement('span');
        this.arrowIcon.className = 'autocomplete-arrow';
        this.arrowIcon.innerHTML = '<svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M2.5 4.5L6 8L9.5 4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>';

        this.trigger.appendChild(this.selectedDisplay);
        this.trigger.appendChild(this.clearButton);
        this.trigger.appendChild(this.arrowIcon);

        this.dropdown = document.createElement('div');
        this.dropdown.className = 'autocomplete-dropdown';
        this.dropdown.style.display = 'none';
        this.dropdown.setAttribute('role', 'listbox');

        const searchContainer = document.createElement('div');
        searchContainer.className = 'autocomplete-search-container';

        this.searchInput = document.createElement('input');
        this.searchInput.type = 'text';
        this.searchInput.className = 'autocomplete-search-input';
        this.searchInput.placeholder = 'Search...';
        this.searchInput.setAttribute('autocomplete', 'off');

        searchContainer.appendChild(this.searchInput);

        this.listElement = document.createElement('div');
        this.listElement.className = 'autocomplete-list';

        this.loadingElement = document.createElement('div');
        this.loadingElement.className = 'autocomplete-loading';
        this.loadingElement.innerHTML = '<span class="autocomplete-spinner"></span> Loading...';

        this.dropdown.appendChild(searchContainer);
        this.dropdown.appendChild(this.listElement);
        this.dropdown.appendChild(this.loadingElement);

        this.input.parentNode.insertBefore(this.wrapper, this.input);
        this.wrapper.appendChild(this.trigger);
        this.wrapper.appendChild(this.dropdown);

        this.input.style.display = 'none';
    }

    bindEvents() {
        this.trigger.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            if (this.isOpen) {
                this.hideDropdown();
            } else {
                this.showDropdown();
            }
        });

        this.trigger.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                if (!this.isOpen) {
                    this.showDropdown();
                }
            } else if (e.key === 'Escape') {
                this.hideDropdown();
            } else if (e.key === 'ArrowDown' && !this.isOpen) {
                e.preventDefault();
                this.showDropdown();
            }
        });

        this.searchInput.addEventListener('input', () => {
            this.currentPage = 0;
            this.hasMore = true;
            this.highlightedIndex = -1;
            this.debouncedSearch();
        });

        this.searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                this.highlightNext();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                this.highlightPrevious();
            } else if (e.key === 'Enter') {
                e.preventDefault();
                if (this.highlightedIndex >= 0 && this.highlightedIndex < this.items.length) {
                    this.selectItem(this.items[this.highlightedIndex]);
                }
            } else if (e.key === 'Escape') {
                this.hideDropdown();
            }
        });

        this.clearButton.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            this.clearSelection();
        });

        document.addEventListener('click', (e) => {
            if (!this.wrapper.contains(e.target)) {
                this.hideDropdown();
            }
        });

        this.listElement.addEventListener('scroll', () => {
            if (this.isLoading || !this.hasMore) return;
            const { scrollTop, scrollHeight, clientHeight } = this.listElement;
            if (scrollTop + clientHeight >= scrollHeight - 20) {
                this.loadMore();
            }
        });
    }

    loadInitialValue() {
        if (this.hiddenInput && this.hiddenInput.value) {
            this.selectedKey = this.hiddenInput.value;
            this.fetchItemLabel(this.hiddenInput.value);
        }
    }

    async fetchItemLabel(key) {
        try {
            const response = await fetch(`/${this.config.adminPath}/autocomplete/${this.config.tableName}/${this.config.columnName}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ search: '', page: 0 }),
            });
            if (!response.ok) throw new Error('Request failed');
            const data = await response.json();
            const item = data.items.find(i => i.key === key);
            if (item) {
                this.selectedLabel = item.label;
                this.selectedDisplay.textContent = item.label;
                this.selectedDisplay.classList.add('has-value');
            }
        } catch (error) {
            console.error('Failed to fetch item label:', error);
        }
    }

    getApiUrl() {
        return `/${this.config.adminPath}/autocomplete/${this.config.tableName}/${this.config.columnName}`;
    }

    debouncedSearch() {
        clearTimeout(this.searchTimeout);
        this.searchTimeout = setTimeout(() => {
            this.search();
        }, this.config.debounceMs);
    }

    async search() {
        if (this.isLoading) return;

        const query = this.searchInput.value.trim();
        this.isLoading = true;
        this.showLoading();

        try {
            const response = await fetch(this.getApiUrl(), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    search: query,
                    page: 0,
                }),
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();
            this.items = data.items;
            this.hasMore = this.items.length < data.totalCount;
            this.currentPage = 0;
            this.highlightedIndex = -1;
            this.renderItems();
        } catch (error) {
            console.error('Autocomplete search failed:', error);
            this.showError('Failed to load results');
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    async loadMore() {
        if (this.isLoading || !this.hasMore) return;

        this.isLoading = true;
        this.showLoading();

        try {
            const query = this.searchInput.value.trim();
            const response = await fetch(this.getApiUrl(), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    search: query,
                    page: this.currentPage + 1,
                }),
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();
            this.items = [...this.items, ...data.items];
            this.hasMore = this.items.length < data.totalCount;
            this.currentPage++;
            this.renderItems();
        } catch (error) {
            console.error('Failed to load more items:', error);
            this.showError('Failed to load more results');
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    renderItems() {
        this.listElement.innerHTML = '';

        if (this.items.length === 0) {
            this.showEmpty();
            return;
        }

        this.items.forEach((item, index) => {
            const itemElement = document.createElement('div');
            itemElement.className = 'autocomplete-item';
            if (item.key === this.selectedKey) {
                itemElement.classList.add('selected');
            }
            if (index === this.highlightedIndex) {
                itemElement.classList.add('highlighted');
            }
            itemElement.textContent = item.label;
            itemElement.dataset.index = index;
            itemElement.setAttribute('role', 'option');
            if (item.key === this.selectedKey) {
                itemElement.setAttribute('aria-selected', 'true');
            }

            itemElement.addEventListener('click', (e) => {
                e.preventDefault();
                this.selectItem(item);
            });

            itemElement.addEventListener('mouseenter', () => {
                this.highlightedIndex = index;
                this.updateHighlight();
            });

            this.listElement.appendChild(itemElement);
        });
    }

    showEmpty() {
        this.listElement.innerHTML = '<div class="autocomplete-empty">No results found</div>';
    }

    showError(message) {
        this.listElement.innerHTML = `<div class="autocomplete-error">${message}</div>`;
    }

    highlightNext() {
        if (this.items.length === 0) return;
        this.highlightedIndex = Math.min(this.highlightedIndex + 1, this.items.length - 1);
        this.updateHighlight();
        this.scrollToHighlighted();
    }

    highlightPrevious() {
        if (this.items.length === 0) return;
        this.highlightedIndex = Math.max(this.highlightedIndex - 1, 0);
        this.updateHighlight();
        this.scrollToHighlighted();
    }

    updateHighlight() {
        const items = this.listElement.querySelectorAll('.autocomplete-item');
        items.forEach((item, index) => {
            if (index === this.highlightedIndex) {
                item.classList.add('highlighted');
            } else {
                item.classList.remove('highlighted');
            }
        });
    }

    scrollToHighlighted() {
        const highlighted = this.listElement.querySelector('.autocomplete-item.highlighted');
        if (highlighted) {
            highlighted.scrollIntoView({ block: 'nearest' });
        }
    }

    selectItem(item) {
        this.selectedKey = item.key;
        this.selectedLabel = item.label;
        this.selectedDisplay.textContent = item.label;
        this.selectedDisplay.classList.add('has-value');
        this.clearButton.style.display = 'flex';

        if (this.hiddenInput) {
            this.hiddenInput.value = item.key;
        }

        this.hideDropdown();
        this.input.dispatchEvent(new Event('change', { bubbles: true }));
    }

    clearSelection() {
        this.selectedKey = null;
        this.selectedLabel = '';
        this.selectedDisplay.textContent = '---';
        this.selectedDisplay.classList.remove('has-value');
        this.clearButton.style.display = 'none';

        if (this.hiddenInput) {
            this.hiddenInput.value = '';
        }

        this.input.dispatchEvent(new Event('change', { bubbles: true }));
    }

    showDropdown() {
        this.dropdown.style.display = 'block';
        this.isOpen = true;
        this.trigger.setAttribute('aria-expanded', 'true');
        this.arrowIcon.classList.add('open');
        this.searchInput.value = '';
        this.highlightedIndex = -1;

        setTimeout(() => this.searchInput.focus(), 50);

        // Load initial results
        this.search();
    }

    hideDropdown() {
        this.dropdown.style.display = 'none';
        this.isOpen = false;
        this.trigger.setAttribute('aria-expanded', 'false');
        this.arrowIcon.classList.remove('open');
    }

    showLoading() {
        this.loadingElement.style.display = 'flex';
    }

    hideLoading() {
        this.loadingElement.style.display = 'none';
    }

    getValue() {
        return this.selectedKey;
    }

    setValue(key, label) {
        this.selectedKey = key;
        this.selectedLabel = label;
        this.selectedDisplay.textContent = label;
        this.selectedDisplay.classList.add('has-value');
        this.clearButton.style.display = 'flex';

        if (this.hiddenInput) {
            this.hiddenInput.value = key;
        }
    }
}

function initializeAutoCompletes() {
    const autoCompleteInputs = document.querySelectorAll('[data-autocomplete]');
    autoCompleteInputs.forEach(input => {
        const config = {
            tableName: input.dataset.autocompleteTable,
            columnName: input.dataset.autocompleteColumn,
            debounceMs: parseInt(input.dataset.autocompleteDebounce) || 300,
            adminPath: input.dataset.autocompleteAdminPath || 'admin',
        };

        new AutoComplete(input, config);
    });
}

document.addEventListener('DOMContentLoaded', initializeAutoCompletes);
