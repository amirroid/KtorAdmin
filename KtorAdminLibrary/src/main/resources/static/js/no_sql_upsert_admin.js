function addSimpleListItem(button) {
    const fieldName = button.dataset.fieldName;
    const fieldType = button.dataset.fieldType;
    const container = button.closest('[data-list-container="true"]');

    const newItem = document.createElement('div');
    newItem.classList.add('dynamic-item');
    newItem.innerHTML = `
        <div class="input-with-buttons">
            <input type="${fieldType}" name="${fieldName}[]" class="form-input"/>
            <div class="list-buttons">
                <button type="button" onclick="removeItem(this)">
                    <i class="material-icons">remove</i>
                </button>
            </div>
        </div>
    `;
    container.appendChild(newItem);
}

function addComplexListItem(button) {
    const fieldName = button.dataset.fieldName;
    const currentPath = button.dataset.currentPath;
    const fields = JSON.parse(button.dataset.fields);
    const container = button.closest('[data-list-container="true"]');

    const newItem = document.createElement('div');
    newItem.classList.add('dynamic-item');

    // Function to recursively render fields
    function renderField(field, path, values = {}) {
        if (field.type === 'list') {
            const listItemsHtml = values[field.name]?.map((item) => {
                if (field.fields.length === 1 && !field.fields[0].name) {
                    return `
                    <div class="dynamic-item">
                        <div class="input-with-buttons">
                            <input type="${field.fields[0].type}" 
                                   name="${path}[]" 
                                   value="${item}" 
                                   class="form-input"/>
                            <div class="list-buttons">
                                <button type="button" onclick="removeItem(this)">
                                    <i class="material-icons">remove</i>
                                </button>
                            </div>
                        </div>
                    </div>`;
                } else {
                    const subFieldsHtml = field.fields.map(subField => {
                        return renderField(subField, `${path}[${field.name}]`, item);
                    }).join('');
                    return `
                    <div class="dynamic-item">
                        ${subFieldsHtml}
                        <div class="list-buttons">
                            <button type="button" onclick="removeItem(this)">
                                <i class="material-icons">remove</i>
                            </button>
                        </div>
                    </div>`;
                }
            }).join('') || '';

            return `
            <fieldset class="nested-container" data-field-name="${field.name}">
                <legend>${field.name} (List)</legend>
                <div id="${field.name}-container" data-list-container="true">
                    ${listItemsHtml}
                    <div class="list-buttons">
                        <button type="button"
                                data-field-name="${field.name}"
                                data-field-type="${field.fields[0]?.type || 'text'}"
                                onclick="${field.fields.length === 1 && !field.fields[0]?.name ? 'addSimpleListItem(this)' : 'addComplexListItem(this)'}">
                            <i class="material-icons">add</i>
                        </button>
                    </div>
                </div>
            </fieldset>`;
        } else {
            return `
            <div class="dynamic-item">
                <label>${field.name}</label>
                <input type="${field.type}" 
                       name="${path}[][${field.name}]" 
                       class="form-input"/>
            </div>`;
        }
    }

    // Render all fields
    const innerHTML = fields.map(field =>
        renderField(field, currentPath)
    ).join('');

    newItem.innerHTML = innerHTML + `
        <div class="list-buttons">
            <button type="button" onclick="removeItem(this)">
                <i class="material-icons">remove</i>
            </button>
        </div>
    `;

    container.insertBefore(newItem, container.lastElementChild);
}

function addSimpleMapItem(button) {
    const fieldName = button.dataset.fieldName;
    const fieldType = button.dataset.fieldType;
    const container = button.closest('[data-map-container="true"]');

    const newItem = document.createElement('div');
    newItem.classList.add('dynamic-item');
    newItem.innerHTML = `
        <div class="input-with-buttons">
            <input type="text" name="${fieldName}-key[]" placeholder="Key" class="form-input"/>
            <input type="${fieldType}" name="${fieldName}-value[]" placeholder="Value" class="form-input"/>
            <div class="list-buttons">
                <button type="button" onclick="removeItem(this)">
                    <i class="material-icons">remove</i>
                </button>
            </div>
        </div>
    `;
    container.appendChild(newItem);
}

function removeItem(button) {
    button.closest('.dynamic-item').remove();
}