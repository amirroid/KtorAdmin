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

    function renderField(field, path) {
        // Handle single-field lists
        if (field.type === 'List' && field.fields.length === 1 && !field.fields[0].fieldName) {
            return `
        <fieldset class="nested-container" data-field-name="${field.fieldName || field.name}">
            <legend>${field.fieldName || field.name} (List)</legend>
            <div id="${field.fieldName || field.name}-container" data-list-container="true">
                ${(field.values || []).map(item => `
                    <div class="dynamic-item">
                        <div class="input-with-buttons">
                            <input type="${field.fields[0].type.fieldType}" 
                                   name="${path}[]" 
                                   value="${item}" 
                                   class="form-input"/>
                            <div class="list-buttons">
                                <button type="button" onclick="removeItem(this)">
                                    <i class="material-icons">remove</i>
                                </button>
                            </div>
                        </div>
                    </div>
                `).join('')}
                <div class="list-buttons">
                    <button type="button"
                            data-field-name="${field.fieldName || field.name}"
                            data-field-type="${field.fields[0].type.fieldType}"
                            onclick="addSimpleListItem(this)">
                        <i class="material-icons">add</i>
                    </button>
                </div>
            </div>
        </fieldset>`;
        }

        // Handle nested lists and maps
        if (field.type === 'List' || field.type === 'Map') {
            return `
        <fieldset class="nested-container" data-field-name="${field.fieldName || field.name}">
            <legend>${field.fieldName || field.name} (${field.type})</legend>
            <div id="${field.fieldName || field.name}-container" data-${field.type.toLowerCase()}-container="true">
                ${(field.values || []).map(item => `
                    <div class="dynamic-item">
                        ${field.fields.map(subField =>
                renderField({
                    ...subField,
                    values: item
                }, `${path}[${field.fieldName || field.name}]`)
            ).join('')}
                        <div class="list-buttons">
                            <button type="button" onclick="removeItem(this)">
                                <i class="material-icons">remove</i>
                            </button>
                        </div>
                    </div>
                `).join('')}
                <div class="list-buttons">
                    <button type="button"
                            data-field-name="${field.fieldName || field.name}"
                            data-current-path="${path}[${field.fieldName || field.name}]"
                            data-fields='${JSON.stringify(field.fields)}'
                            onclick="addComplexListItem(this)">
                        <i class="material-icons">add</i>
                    </button>
                </div>
            </div>
        </fieldset>`;
        }

        // Handle simple fields
        return `
    <div class="dynamic-item">
        <label>${field.fieldName || field.name}</label>
      <br />
        ${field.fields.length}
        <input type="${field.type.fieldType || field.type}" 
               name="${path}[${field.fieldName || field.name}]" 
               value="${field.values ? field.values[0] : ''}"
               class="form-input"/>
    </div>`;
    }    // Render all fields
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