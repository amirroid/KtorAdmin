function addSimpleListItem(button) {
    const fieldName = button.dataset.fieldName;
    const fieldType = button.dataset.fieldType;
    const container = button.closest('[data-list-container="true"]');

    const newItem = document.createElement('div');
    newItem.classList.add('dynamic-item');
    newItem.innerHTML = `
        <input type="${fieldType}" name="${fieldName}[]" class="form-input"/>
        <button type="button" onclick="removeItem(this)">-</button>
    `;
    container.appendChild(newItem);
}

function addComplexListItem(button) {
    const fieldName = button.dataset.fieldName;
    const fields = JSON.parse(button.dataset.fields);
    const container = button.closest('[data-list-container="true"]');

    const newItem = document.createElement('div');
    newItem.classList.add('dynamic-item');

    const innerHTML = fields.map(field => `
            <label>${field.name}</label>
            <input type="${field.type}"
                   name="${fieldName}[][${field.name}]"
                   class="form-input"/>
        `).join('');

    newItem.innerHTML = innerHTML + `
            <button type="button" onclick="removeItem(this)">-</button>
        `;
    container.appendChild(newItem);
}

function addSimpleMapItem(button) {
    const fieldName = button.dataset.fieldName;
    const fieldType = button.dataset.fieldType;
    const container = button.closest('[data-map-container="true"]');

    const newItem = document.createElement('div');
    newItem.classList.add('dynamic-item');
    newItem.innerHTML = `
            <input type="text" name="${fieldName}-key[]" placeholder="Key" class="form-input"/>
            <input type="${fieldType}" name="${fieldName}-value[]" placeholder="Value" class="form-input"/>
            <button type="button" onclick="removeItem(this)">-</button>
        `;
    container.appendChild(newItem);
}

function removeItem(button) {
    button.parentElement.remove();
}