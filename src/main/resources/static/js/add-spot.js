// ==========================================================================
// MAP INITIALIZATION SAFE-LOADER
// ==========================================================================

// 1. Expose it globally so the Google Maps script tag callback can find it directly
window.initAddSpotMap = function() {
    const mapElement = document.getElementById('mapCanvas');
    if (!mapElement) return;

    // Center point for India
    const indiaCenter = { lat: 20.5937, lng: 78.9629 };

    const map = new google.maps.Map(mapElement, {
        center: indiaCenter,
        zoom: 5,
        mapTypeControl: false,
        streetViewControl: false
    });

    const marker = new google.maps.Marker({
        map: map,
        anchorPoint: new google.maps.Point(0, -29),
        draggable: true,
        visible: false
    });

    const input = document.getElementById('mapSearchInput');
    if (input) {
        const autocomplete = new google.maps.places.Autocomplete(input, {
            fields: ["geometry", "name", "address_components", "formatted_address", "url", "formatted_phone_number"]
        });
        
        autocomplete.setComponentRestrictions({ country: "in" });
        autocomplete.bindTo("bounds", map);

        autocomplete.addListener('place_changed', function() {
            marker.setVisible(false);
            const place = autocomplete.getPlace();
            if (!place.geometry || !place.geometry.location) return;

            if (place.geometry.viewport) {
                map.fitBounds(place.geometry.viewport);
            } else {
                map.setCenter(place.geometry.location);
                map.setZoom(17);
            }
            marker.setPosition(place.geometry.location);
            marker.setVisible(true);

            fillFormFields(place);
        });
    }

    // Direct manual map clicks
    map.addListener("click", function(e) {
        marker.setPosition(e.latLng);
        marker.setVisible(true);
        clearAddressFields();
    });

    marker.addListener('dragend', function() {
        const latLng = marker.getPosition();
        const geocoder = new google.maps.Geocoder();
        geocoder.geocode({ location: latLng }, function(results, status) {
            if (status === 'OK' && results[0]) {
                fillFormFields(results[0]);
                if (input) input.value = results[0].formatted_address;
            }
        });
    });
};

// 2. Fallback check on DOMContentLoaded in case the script loads faster than expected
document.addEventListener("DOMContentLoaded", function () {
    if (window.google && window.google.maps) {
        initAddSpotMap();
    }
});

function clearAddressFields() {
    if (document.getElementById("spotName")) document.getElementById("spotName").value = "";
    if (document.getElementById("spotPlace")) document.getElementById("spotPlace").value = "";
    if (document.getElementById("district")) document.getElementById("district").value = "";
    if (document.getElementById("state")) document.getElementById("state").value = "";
    if (document.getElementById("phone")) document.getElementById("phone").value = "";
    if (document.getElementById("mapLink")) document.getElementById("mapLink").value = "";
}

function fillFormFields(place) {
    clearAddressFields();

    let name = place.name || '';
    let fullAddress = place.formatted_address || '';
    let locality = '';
    let district = '';
    let state = '';
    let phoneVal = place.formatted_phone_number || '';
    let urlVal = place.url || '';

    if (place.name) {
        document.getElementById('spotName').value = place.name;
    }

    if (place.address_components) {
        for (const component of place.address_components) {
            const componentType = component.types[0];

            switch (componentType) {
                case "locality":
                case "sublocality_level_1":
                    if (!locality) locality = component.long_name;
                    break;
                case "administrative_area_level_2":
                case "administrative_area_level_3":
                    if (!district) district = component.long_name;
                    break;
                case "administrative_area_level_1":
                    state = component.long_name;
                    break;
            }
        }
    }

    // Fallback String Scraper if district component is omitted
    if (!district && place.formatted_address) {
        const addressParts = place.formatted_address.split(',');
        if (addressParts.length >= 3) {
            district = addressParts[addressParts.length - 3].trim();
        }
    }

    if (placeField = document.getElementById('spotPlace')) {
        placeField.value = fullAddress;
    }
    if (locality && document.getElementById('spotPlace')) {
        document.getElementById('spotPlace').value = fullAddress;
    }
    if (district && document.getElementById('district')) {
        document.getElementById('district').value = district;
    }
    if (state && document.getElementById('state')) {
        document.getElementById('state').value = state;
    }
    if (phoneVal && document.getElementById('phone')) {
        document.getElementById('phone').value = phoneVal;
    }
    if (document.getElementById('mapLink')) {
        document.getElementById('mapLink').value = urlVal;
    }
}

// ==========================================================================
// CUSTOM DROPDOWN & CANCEL BUTTON LOGIC
// ==========================================================================

document.addEventListener('DOMContentLoaded', function() {
    const toggle = document.getElementById('categoryToggle');
    const menu = document.getElementById('categoryMenu');
    const checkboxes = menu ? menu.querySelectorAll('input[type="checkbox"]') : [];
    const toggleText = toggle ? toggle.querySelector('span') : null;

    if (toggle && menu) {
        toggle.addEventListener('click', (e) => {
            e.stopPropagation();
            menu.classList.toggle('show');
        });

        checkboxes.forEach(cb => {
            cb.addEventListener('change', () => {
                const selected = Array.from(checkboxes)
                    .filter(i => i.checked)
                    .map(i => i.value);
                
                if (toggleText) {
                    toggleText.textContent = selected.length > 0 
                        ? selected.join(', ') 
                        : 'Select Categories...';
                }
            });
        });

        document.addEventListener('click', (e) => {
            if (!toggle.contains(e.target) && !menu.contains(e.target)) {
                menu.classList.remove('show');
            }
        });
    }

    const cancelBtn = document.getElementById('cancelBtn');
    if (cancelBtn) {
        cancelBtn.addEventListener('click', function() {
            const form = document.querySelector('form');
            if (form) form.reset();
            
            const mapInput = document.getElementById('mapSearchInput');
            if (mapInput) mapInput.value = '';

            if (toggleText) {
                toggleText.textContent = 'Select Categories...';
            }

            if (menu) {
                menu.classList.remove('show');
            }
        });
    }
});

// ==========================================================================
// SCROLL POSITION RESTORATION & AUTO-DISMISS SUCCESS MESSAGE
// ==========================================================================

document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("form");

    // 1. Save scroll position right before the form submits and reloads
    if (form) {
        form.addEventListener("submit", function () {
            sessionStorage.setItem("scrollPos", window.scrollY);
        });
    }

    // 2. Instantly restore the scroll position upon page reload if it exists
    const savedScrollPos = sessionStorage.getItem("scrollPos");
    if (savedScrollPos !== null) {
        window.scrollTo(0, parseInt(savedScrollPos));
        sessionStorage.removeItem("scrollPos");
    }

    // 3. Automatically fade out and hide the success message after 4 seconds
    const successAlert = document.querySelector(".success-alert");
    if (successAlert) {
        setTimeout(function () {
            successAlert.style.transition = "opacity 0.5s ease";
            successAlert.style.opacity = "0";
            
            setTimeout(function () {
                successAlert.style.display = "none";
            }, 500); // Matches the 0.5s transition time
        }, 4000); // 4 seconds delay
    }
});