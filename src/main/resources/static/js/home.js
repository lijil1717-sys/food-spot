// ==========================================================================
// ENVIRONMENT API CONFIGURATION (Local vs. Live Hosting Automatic Switch)
// ==========================================================================
const API_BASE_URL = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
    ? 'http://localhost:8081'  // Your local Java Spring Boot backend port
    : '';                        // Empty string for production (if frontend & backend share domain)

// ==========================================================================
// CARD EXPANSION & ZOOM UI INTERACTIONS (Event Delegation for AJAX)
// ==========================================================================
document.addEventListener('click', (e) => {
    const overlay = document.getElementById('card-overlay');

    // 1. Handle clicking a card to expand it (ignoring clicks on buttons or links)
    const card = e.target.closest('.food-spot-card');
    if (card && !e.target.closest('a') && !e.target.closest('button')) {
        if (card.classList.contains('is-expanded')) return;

        card.classList.add('is-expanded');
        
        setTimeout(() => {
            if (overlay) overlay.classList.add('active');
            card.classList.add('active');
        }, 10);
        return;
    }

    // 2. Close expanded card when clicking outside on the dim background overlay
    if (e.target && e.target.id === 'card-overlay') {
        const activeCard = document.querySelector('.food-spot-card.is-expanded, .food-spot-card.is-zoomed');
        
        if (activeCard) {
            activeCard.classList.remove('active', 'is-zoomed');
            if (overlay) overlay.classList.remove('active');

            setTimeout(() => {
                activeCard.classList.remove('is-expanded');
            }, 300); 
        }
    }
});

function zoomCardAndShowPhotos(btn) {
    const card = btn.closest('.food-spot-card');
    const overlay = document.getElementById('card-overlay');

    if (card) {
        card.classList.add('is-zoomed');
    }
    if (overlay) {
        overlay.classList.add('active');
    }
}

function closeCardZoom(buttonElement) {
    const zoomedCard = document.querySelector('.food-spot-card.is-zoomed');
    
    if (zoomedCard) {
        zoomedCard.classList.remove('is-zoomed');
        zoomedCard.classList.add('is-expanded', 'active');
    }
}

// ==========================================================================
// STATIC UI INTERACTIONS & INITIALIZATIONS
// ==========================================================================
document.addEventListener("DOMContentLoaded", () => {
    const sizePills = document.querySelectorAll(".size-pill");
    const logoutBtn = document.getElementById("logoutBtn");

    // Product Weight / Item Selection Toggles
    sizePills.forEach(pill => {
        pill.addEventListener("click", function() {
            const siblings = this.parentElement.querySelectorAll(".size-pill");
            siblings.forEach(s => s.classList.remove("active"));
            this.classList.add("active");
        });
    });

    // Session Log-Out Redirection
    if (logoutBtn) {
        logoutBtn.addEventListener("click", (e) => {
            e.preventDefault();
            window.location.href = "index.html";
        });
    }

    // Dynamic Navbar Scroll Styling Class
    const navbar = document.querySelector('.voldog-navbar');
    if (navbar) {
        window.addEventListener('scroll', () => {
            if (window.scrollY > 50) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
        });
    }

    // ==========================================================================
    // AUTO-LOAD ALL SPOTS UPON LOGIN / PAGE LOAD (IF CONTAINER IS EMPTY)
    // ==========================================================================
    const spotsContainer = document.getElementById('food-spots-container');
    const urlParams = new URLSearchParams(window.location.search);
    
    // If no specific district or filter is set in the URL, and container is empty, load all spots automatically from /home
    if (spotsContainer && spotsContainer.children.length === 0 && !urlParams.has('district') && !urlParams.has('filter')) {
        loadSpots(null, '/home');
    }
});

// ==========================================================================
// COMBINED FILTER & DISTRICT CONTROLS FOR AJAX
// ==========================================================================
function handleDistrictSelection(selectEl) {
    const selectedDistrict = selectEl.value;
    const urlParams = new URLSearchParams(window.location.search);
    const currentFilter = urlParams.get('filter');

    let targetUrl = '/home';
    let params = [];

    // If a valid district is chosen, add it to query params
    if (selectedDistrict && selectedDistrict.trim() !== '') {
        params.push('district=' + encodeURIComponent(selectedDistrict));
    }

    // Preserve active sub-filter if it exists
    if (currentFilter) {
        params.push('filter=' + encodeURIComponent(currentFilter));
    }

    if (params.length > 0) {
        targetUrl += '?' + params.join('&');
    }

    loadSpots(event, targetUrl);
}

function onFilterClick(filterType) {
    const urlParams = new URLSearchParams(window.location.search);
    const currentDistrict = urlParams.get('district');
    const existingFilter = urlParams.get('filter');

    // Toggle behavior: if clicking the already active filter, remove it
    let targetFilter = (existingFilter === filterType) ? null : filterType;

    // --- INSTANT VISUAL FEEDBACK (UI TWEAK) ---
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    
    if (targetFilter) {
        // Find the button that was clicked and add 'active'
        const clickedButton = event.currentTarget || event.target.closest('button');
        if (clickedButton) {
            clickedButton.classList.add('active');
        }
    } else {
        // If toggled off, fallback to highlighting "All Spots"
        const allSpotsBtn = document.getElementById('all-spots-btn');
        if (allSpotsBtn) allSpotsBtn.classList.add('active');
    }
    // ------------------------------------------

    let targetUrl = '/home';
    let params = [];

    if (currentDistrict) {
        params.push('district=' + encodeURIComponent(currentDistrict));
    }
    if (targetFilter) {
        params.push('filter=' + encodeURIComponent(targetFilter));
    }

    if (params.length > 0) {
        targetUrl += '?' + params.join('&');
    }

    loadSpots(event, targetUrl);
}

// ==========================================================================
// GOOGLE MAPS ROUTING & MAP PIN MARKERS ONLY
// ==========================================================================
let map;
let directionsService;
let directionsRenderer;
let sourceAutocomplete;
let destinationAutocomplete;

let startMarker = null;
let endMarker = null;
let foodMarkers = [];

function initMap() {
    directionsService = new google.maps.DirectionsService();
    const keralaCenter = { lat: 10.8505, lng: 76.2711 };

    map = new google.maps.Map(document.getElementById("map"), {
        zoom: 7,
        center: keralaCenter,
        mapTypeControl: false,
        fullscreenControl: false,
        streetViewControl: false
    });

    directionsRenderer = new google.maps.DirectionsRenderer({
        polylineOptions: {
            strokeColor: "#4A6B53", 
            strokeWeight: 6
        }
    });
    directionsRenderer.setMap(map);

    initAutocompleteFields();
}

function initAutocompleteFields() {
    const fromInput = document.getElementById('routeFrom');
    const toInput = document.getElementById('routeTo');

    if (!fromInput || !toInput) return;

    const options = {
        fields: ["formatted_address", "geometry"],
        types: ["geocode", "establishment"]
    };

    sourceAutocomplete = new google.maps.places.Autocomplete(fromInput, options);
    destinationAutocomplete = new google.maps.places.Autocomplete(toInput, options);

    sourceAutocomplete.bindTo("bounds", map);
    destinationAutocomplete.bindTo("bounds", map);

    sourceAutocomplete.addListener("place_changed", () => {
        const place = sourceAutocomplete.getPlace();
        if (!place.geometry || !place.geometry.location) return;

        clearFoodMarkers();
        directionsRenderer.setDirections({ routes: [] });

        if (startMarker) startMarker.setMap(null);
        startMarker = new google.maps.Marker({
            position: place.geometry.location,
            map: map,
            title: "Starting Point"
        });
        map.panTo(place.geometry.location);
        map.setZoom(14); 
    });

    destinationAutocomplete.addListener("place_changed", () => {
        const place = destinationAutocomplete.getPlace();
        if (!place.geometry || !place.geometry.location) return;

        clearFoodMarkers();
        directionsRenderer.setDirections({ routes: [] });

        if (endMarker) endMarker.setMap(null);
        endMarker = new google.maps.Marker({
            position: place.geometry.location,
            map: map,
            title: "Destination Point"
        });
        map.panTo(place.geometry.location);
        map.setZoom(14);
    });

    const searchBtn = document.querySelector('.travel-horizontal-search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', calculateRequestedRoute);
    }
}

function calculateRequestedRoute() {
    const originText = document.getElementById('routeFrom').value.trim();
    const destinationText = document.getElementById('routeTo').value.trim();

    if (!originText || !destinationText) {
        alert("Please select both a source and a destination first!");
        return;
    }

    const request = {
        origin: originText,
        destination: destinationText,
        travelMode: google.maps.TravelMode.DRIVING
    };

    directionsService.route(request, (result, status) => {
        if (status === google.maps.DirectionsStatus.OK) {
            if (startMarker) startMarker.setMap(null);
            if (endMarker) endMarker.setMap(null);
            clearFoodMarkers();

            directionsRenderer.setDirections(result);

            const bounds = new google.maps.LatLngBounds();
            const route = result.routes[0];
            let pathCoordinates = [];

            route.legs.forEach(leg => {
                bounds.extend(leg.start_location);
                bounds.extend(leg.end_location);

                leg.steps.forEach(step => {
                    step.path.forEach(latLng => {
                        pathCoordinates.push([latLng.lng(), latLng.lat()]);
                    });
                });
            });
            map.fitBounds(bounds);

            getFoodSpotsFromJava(pathCoordinates);

        } else {
            alert("Routing calculation failed: " + status);
        }
    });
}

function getFoodSpotsFromJava(routeCoordinates) {
    fetch(API_BASE_URL + '/api/auth/food-spots-along-route', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ coordinates: routeCoordinates })
    })
    .then(response => response.json())
    .then(foodSpots => {
        displayMapMarkers(foodSpots);
    })
    .catch(err => console.error("Error loading restaurant markers:", err));
}
function displayMapMarkers(spots) {
    spots.forEach(spot => {
        let lat = spot.latitude;
        let lng = spot.longitude;

        if (!lat && spot.location && spot.location.coordinates) {
            lng = spot.location.coordinates[0];
            lat = spot.location.coordinates[1];
        }

        if (!lat || !lng) return;

        const marker = new google.maps.Marker({
            position: { lat: parseFloat(lat), lng: parseFloat(lng) },
            map: map,
            title: spot.name,
            icon: {
                url: 'data:image/svg+xml;charset=UTF-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%2240%22%20height%3D%2240%22%20viewBox%3D%220%200%2040%2040%22%3E%3Cfilter%20id%3D%22shadow%22%20x%3D%22-20%25%22%20y%3D%22-20%25%22%20width%3D%22140%25%22%20height%3D%22140%25%22%3E%3CfeDropShadow%20dx%3D%220%22%20dy%3D%222%22%20stdDeviation%3D%222%22%20flood-opacity%3D%220.3%22%2F%3E%3C%2Ffilter%3E%3Ccircle%20cx%3D%2220%22%20cy%3D%2220%22%20r%3D%2217%22%20fill%3D%22%23ffffff%22%20stroke%3D%22%23000000%22%20stroke-width%3D%222%22%20filter%3D%22url(%23shadow)%22%2F%3E%3Cg%20transform%3D%22translate(11%2C%209)%22%20fill%3D%22%23f97316%22%3E%3Cpath%20d%3D%22M4%201v6c0%20.65.65%201%201%201s1-.45%201-1V1h1v6c0%20.55.45%201%201%201s1-.45%201-1V1h1v7c0%201.1-.9%202-2%202v10H6V10c-1.1%200-2-.9-2-2V1H3z%22%2F%3E%3Cpath%20d%3D%22M14%201c-.55%200-1%20.45-1%201v11c0%201.1.9%202%202%202h1v5h2V2.5C18%201.67%2017.33%201%2016.5%201H14z%22%2F%3E%3C%2Fg%3E%3C%2Fsvg%3E',
                scaledSize: new google.maps.Size(40, 40),
                anchor: new google.maps.Point(20, 20)
            }
        });

        // 1. Small card content for the Google Maps InfoWindow
        const smallCardHtml = `
            <div id="small-card-${spot.id}" class="map-small-card" style="color:#1c2d24; font-family:sans-serif; padding:5px; max-width:210px; cursor:pointer;" title="Click to view expanded card">
                ${spot.photoUrl ? `<img src="${spot.photoUrl}" alt="${spot.name || 'Food Spot'}" style="width:100%; height:110px; object-fit:cover; border-radius:6px; margin-bottom:6px;">` : ''}
                <h4 style="margin:0 0 4px 0; color:#4A6B53; font-size:14px;">${spot.name || 'Food Spot'}</h4>
                <p style="margin:0 0 4px 0; font-size:12px;"><b>Specialty:</b> ${spot.special || 'N/A'}</p>
                <p style="margin:0 0 6px 0; font-size:11px; color:#666;">📍 ${spot.exactPlace || ''} ${spot.place || ''}</p>
                <span style="font-size:11px; color:#f97316; font-weight:bold; text-decoration:underline;">Click to expand card &rarr;</span>
            </div>
        `;

        const infoWindow = new google.maps.InfoWindow({
            content: smallCardHtml
        });

        marker.addListener("click", () => {
            // Close any previously open info windows if needed, then open this one
            infoWindow.open(map, marker);

            // Once the InfoWindow is rendered into the DOM, attach a click listener to the small card itself
            google.maps.event.addListenerOnce(infoWindow, 'domready', () => {
                const smallCardElement = document.getElementById(`small-card-${spot.id}`);
                if (smallCardElement) {
                    smallCardElement.addEventListener('click', () => {
                        // ---> TRIGGER YOUR EXPANDED CARD HERE <---
                        // Replace 'openExpandedCard(spot)' with whatever function your main UI uses to show the expanded card
                        if (typeof openExpandedCard === 'function') {
                            openExpandedCard(spot);
                        } else {
                            // Fallback if your app uses a global modal launcher or data attribute selector
                            showMainSpotExpandedCard(spot);
                        }
                    });
                }
            });
        });

        foodMarkers.push(marker);
    });
}

// Fallback or helper function to display your main app's expanded card view layout overlay
function showMainSpotExpandedCard(spot) {
    let modalOverlay = document.getElementById('global-spot-expanded-overlay');
    if (!modalOverlay) {
        modalOverlay = document.createElement('div');
        modalOverlay.id = 'global-spot-expanded-overlay';
        modalOverlay.style.cssText = `
            position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(0,0,0,0.6); z-index: 10000; display: flex;
            justify-content: center; align-items: center; padding: 20px;
        `;
        document.body.appendChild(modalOverlay);
    }

    // Fallback coordinates/details calculation if missing
    let lat = spot.latitude || (spot.location && spot.location.coordinates ? spot.location.coordinates[1] : '');
    let lng = spot.longitude || (spot.location && spot.location.coordinates ? spot.location.coordinates[0] : '');

    modalOverlay.innerHTML = `
        <div style="background: #fff; width: 100%; max-width: 500px; border-radius: 12px; padding: 20px; box-shadow: 0 10px 25px rgba(0,0,0,0.3); position: relative; font-family: sans-serif; max-height: 90vh; overflow-y: auto;">
            <button onclick="document.getElementById('global-spot-expanded-overlay').remove()" style="position: absolute; top: 15px; right: 15px; background: none; border: none; font-size: 20px; cursor: pointer; color: #333;">&times;</button>
            
            ${spot.photoUrl ? `<img src="${spot.photoUrl}" alt="${spot.name}" style="width: 100%; height: 220px; object-fit: cover; border-radius: 8px; margin-bottom: 12px;">` : ''}
            
            <h2 style="margin: 0 0 8px 0; color: #4A6B53;">${spot.name || 'Food Spot'}</h2>
            
            <p style="margin: 0 0 8px 0; font-size: 14px;"><b>Specialty:</b> ${spot.special || 'N/A'}</p>
            <p style="margin: 0 0 8px 0; font-size: 13px; color: #666;"><b>Location:</b> ${spot.exactPlace || ''}, ${spot.place || ''} (${spot.district || ''})</p>
            
            <p style="margin: 0 0 8px 0; font-size: 13px;"><b>Category:</b> 
                ${spot.veg ? 'Veg ' : ''} ${spot.nonveg ? 'Non-Veg ' : ''} ${spot.coolbar ? 'Coolbar ' : ''} ${spot.stayIn ? 'Stay-In' : ''}
            </p>

           

            <p style="margin: 0 0 8px 0; font-size: 13px;"><b>Category:</b> 
                <strong>📌 </strong> 
                <a href="https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(spot.name || '')},${encodeURIComponent(spot.place || '')},${encodeURIComponent(spot.district || '')}&query_place_id=&ll=${lat},${lng}" target="_blank" class="map-link" onclick="event.stopPropagation();" style="color: #4A6B53; text-decoration: underline; font-weight: bold;">
                    🗺️ Open in Google Maps
                </a>
            </p>

        
            
            <div style="display: flex; gap: 10px;">
                <button onclick="document.getElementById('global-spot-expanded-overlay').remove()" style="flex: 1; background: #e5e7eb; color: #374151; border: none; padding: 10px; border-radius: 6px; cursor: pointer; font-weight: bold;">Close</button>
            </div>
        </div>
    `;
    modalOverlay.style.display = 'flex';
}
function clearFoodMarkers() {
    foodMarkers.forEach(m => m.setMap(null));
    foodMarkers = [];
}

function loadSpots(event, url) {
    if (event) {
        event.preventDefault();
    }

    const currentScrollY = window.scrollY;
    const separator = url.includes('?') ? '&' : '?';
    const ajaxUrl = url + separator + 'ajax=true';

    fetch(ajaxUrl)
        .then(response => {
            // ---> SESSION EXPIRED CHECK <---
            if (response.status === 401 || (response.redirected && response.url.includes('/foodspot'))) {
                window.location.href = '/foodspot';
                return null;
            }
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.text();
        })
        .then(html => {
            if (!html) return; // Exit if session expired and redirected

            // 1. Inject the fresh fragment HTML into the wrapper
            document.getElementById('food-spots-section-wrapper').innerHTML = html;
            window.history.pushState({}, '', url);

            // 2. Wrap rating re-initialization in a brief timeout to let the DOM settle and paint correctly
            setTimeout(() => {
                if (typeof renderSpotRatings === 'function') {
                    renderSpotRatings();
                }
            }, 30);

            const urlParams = new URLSearchParams(url.split('?')[1] || '');
            const district = urlParams.get('district');
            const filter = urlParams.get('filter');

            // 3. Sync District Dropdown state
            const selectEl = document.getElementById('district-select');
            if (selectEl) {
                selectEl.value = district || "";
                selectEl.classList.toggle('active', !!district);
            }

            // 4. Sync "All Spots" Button state
            const allSpotsBtn = document.getElementById('all-spots-btn');
            if (allSpotsBtn) {
                allSpotsBtn.classList.toggle('active', !district && !filter);
            }

            // 5. Sync Filter Button states
            const topSpotsBtn = document.getElementById('top-spots-btn');
            if (topSpotsBtn) {
                topSpotsBtn.classList.toggle('active', filter === 'top');
            }

            const stayInBtn = document.getElementById('stay-in-btn');
            if (stayInBtn) {
                stayInBtn.classList.toggle('active', filter === 'stayin');
            }

            const nonVegBtn = document.getElementById('non-veg-btn');
            if (nonVegBtn) {
                nonVegBtn.classList.toggle('active', filter === 'nonveg');
            }

            const vegBtn = document.getElementById('veg-btn');
            if (vegBtn) {
                vegBtn.classList.toggle('active', filter === 'veg');
            }

            const coolbarBtn = document.getElementById('coolbar-btn');
            if (coolbarBtn) {
                coolbarBtn.classList.toggle('active', filter === 'coolbar');
            }

            const savedSpotsBtn = document.getElementById('saved-spots-btn');
            if (savedSpotsBtn) {
                savedSpotsBtn.classList.toggle('active', filter === 'saved');
            }

            currentFoodPage = 0; 
            if (typeof updateFoodSpotsPagination === 'function') {
                updateFoodSpotsPagination();
            }

            requestAnimationFrame(() => {
                window.scrollTo({
                    top: currentScrollY,
                    behavior: 'instant'
                });
            });
        })
        .catch(error => {
            console.error('Error loading spots via AJAX:', error);
        });
}

// Synchronize UI if user uses browser Back / Forward buttons
window.addEventListener('popstate', () => {
    loadSpots(null, window.location.pathname + window.location.search);
});

// ==========================================================================
// UNIFIED NAVBAR ACTIVE STATE & SCROLL-SPY CONTROLLER (Instant Scroll Tracking)
// ==========================================================================
document.addEventListener("DOMContentLoaded", () => {
    const sections = document.querySelectorAll(".section-nav");
    // Only target standard navbar links, excluding special buttons like the profile menu
    const menuItems = document.querySelectorAll(".nav-menu .menu-item:not(#profileMenuBtn)");

    let isManualClick = false;

    // 1. Click Handler: Instant color update + smooth scroll
    menuItems.forEach(item => {
        item.addEventListener("click", (e) => {
            const href = item.getAttribute("href");
            if (href && href.startsWith("#") && href.length > 1) {
                e.preventDefault();
                isManualClick = true;

                menuItems.forEach(nav => {
                    nav.classList.remove("active");
                    nav.style.color = "#000000";
                });
                item.classList.add("active");
                item.style.color = "#f97316";

                const targetSection = document.querySelector(href);
                if (targetSection) {
                    targetSection.scrollIntoView({ behavior: "smooth" });
                    setTimeout(() => {
                        isManualClick = false;
                    }, 800);
                }
            }
        });
    });

    // 2. Instant Scroll Position Checker
    window.addEventListener("scroll", () => {
        if (isManualClick) return;

        let currentSectionId = "";
        const scrollPosition = window.scrollY + 200; // Offset to catch sections early

        // Check if user has scrolled to the absolute bottom of the page
        const isAtBottom = (window.innerHeight + window.scrollY) >= document.documentElement.scrollHeight - 50;

        if (isAtBottom && sections.length > 0) {
            // Force the last section ("contact-section") active when at the bottom
            currentSectionId = sections[sections.length - 1].getAttribute("id");
        } else {
            sections.forEach(section => {
                const sectionTop = section.offsetTop;
                const sectionHeight = section.offsetHeight;

                if (scrollPosition >= sectionTop && scrollPosition < sectionTop + sectionHeight) {
                    currentSectionId = section.getAttribute("id");
                }
            });
        }

        if (currentSectionId) {
            menuItems.forEach(link => {
                const linkHref = link.getAttribute("href");
                if (linkHref === `#${currentSectionId}`) {
                    link.classList.add("active");
                    link.style.color = "#f97316"; // Active orange
                } else {
                    link.classList.remove("active");
                    link.style.color = "#000000"; // Default black
                }
            });
        }
    });
});

// ==========================================================================
// STYLISH FOOD SPOTS PAGINATION & SLIDER CONTROLLER
// ==========================================================================
let currentFoodPage = 0;

function updateFoodSpotsPagination() {
    const cards = document.querySelectorAll('#food-spots-container .food-spot-card');
    if (cards.length === 0) return;

    const totalPages = Math.ceil(cards.length / 8);
    
    // Bounds check
    if (currentFoodPage >= totalPages) currentFoodPage = totalPages - 1;
    if (currentFoodPage < 0) currentFoodPage = 0;

    // Show/Hide cards for current page
    cards.forEach((card, index) => {
        const cardPage = Math.floor(index / 8);
        card.style.display = (cardPage === currentFoodPage) ? "" : "none";
    });

    // Populate ALL pagination number containers found on the page (both top and bottom)
    const numberContainers = document.querySelectorAll('.pagination-numbers-flex');
    numberContainers.forEach(numbersContainer => {
        numbersContainer.innerHTML = "";
        for (let i = 0; i < totalPages; i++) {
            const pageNumBtn = document.createElement('button');
            pageNumBtn.type = "button";
            pageNumBtn.className = `page-num-pill ${i === currentFoodPage ? 'active' : ''}`;
            pageNumBtn.textContent = i + 1;
            pageNumBtn.onclick = () => goToPage(i);
            numbersContainer.appendChild(pageNumBtn);
        }
    });

    // Toggle Previous / Next arrow buttons state for all pagination bars
    const prevBtns = document.querySelectorAll('.stylish-pagination-controls .prev-btn');
    const nextBtns = document.querySelectorAll('.stylish-pagination-controls .next-btn');
    
    prevBtns.forEach(btn => btn.disabled = currentFoodPage === 0);
    nextBtns.forEach(btn => btn.disabled = currentFoodPage >= totalPages - 1);
}

function slideFoodSpots(direction) {
    currentFoodPage += direction;
    updateFoodSpotsPagination();
    scrollToSpotsTop();
}

function goToPage(pageNum) {
    currentFoodPage = pageNum;
    updateFoodSpotsPagination();
    scrollToSpotsTop();
}

// Run on page load
document.addEventListener("DOMContentLoaded", () => {
    updateFoodSpotsPagination();
});

// ==========================================================================
// SCROLL TO SPOTS SECTION TOP ON PAGINATION CHANGE
// ==========================================================================
function scrollToSpotsTop() {
    const spotsSection = document.getElementById('spots-section');
    if (spotsSection) {
        spotsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
        // Fallback to title if the ID isn't found
        const titleEl = document.querySelector('.display-division .section-title');
        if (titleEl) {
            titleEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }
}
// ==========================================================================
// RATING & REVIEW AJAX SUBMISSION ENGINE
// ==========================================================================
function onStarClick(starElement) {
    const starRow = starElement.closest('.star-rating-row') || starElement.parentElement;
    const value = parseInt(starElement.getAttribute('data-value'), 10);
    
    // Find the hidden rating value input inside this specific row
    const hiddenInput = starRow.querySelector('.selectedRatingValue') || starRow.querySelector('#selectedRatingValue');
    
    const currentRating = hiddenInput ? parseInt(hiddenInput.value, 10) || 0 : 0;
    const stars = starRow.querySelectorAll('.star');

    let newRating = value;

    // If the user clicks the exact same star that is already selected, deselect everything (set to 0)
    if (currentRating === value) {
        newRating = 0;
        if (hiddenInput) {
            hiddenInput.value = "0";
        }
        stars.forEach(s => s.classList.remove('filled'));
        return;
    }
    
    // Otherwise, set the new rating value
    if (hiddenInput) {
        hiddenInput.value = newRating;
    }
    
    // Highlight the correct stars up to the clicked one
    stars.forEach(s => {
        if (parseInt(s.getAttribute('data-value'), 10) <= newRating) {
            s.classList.add('filled');
        } else {
            s.classList.remove('filled');
        }
    });
}

function submitReviewAjax(event, form) {
    event.preventDefault();
    event.stopPropagation();

    // Prevent rapid double-clicks
    if (form.getAttribute('data-submitting') === 'true') {
        return;
    }

    const spotId = form.querySelector('input[name="spotId"]').value;
    const ratingInput = form.querySelector('.selectedRatingValue') || form.querySelector('#selectedRatingValue');
    const rating = ratingInput ? parseInt(ratingInput.value) : 0;
    const commentInput = form.querySelector('.review-textarea');
    const comment = commentInput ? commentInput.value : "";

    // Basic validation check
    if (!rating || rating == 0) {
        alert("Please select a star rating before submitting.");
        return;
    }

    const submitBtn = form.querySelector('button[type="submit"]') || form.querySelector('.submit-review-btn');
    
    form.setAttribute('data-submitting', 'true');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.style.opacity = "0.5";
    }

    fetch(API_BASE_URL + '/api/auth/submit-review', {  
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            spotId: spotId,
            ratings: rating,
            review: comment
        })
    })
    .then(async response => {
        const contentType = response.headers.get("content-type");
        const data = contentType && contentType.includes("application/json") ? await response.json() : {};

        if (response.ok) {
            const successDiv = form.querySelector('.review-success-msg');
            if (successDiv) {
                successDiv.style.display = 'block';
                successDiv.textContent = data.message || 'Review saved successfully!';
            }

            // ==========================================
            // LIVE UPDATE PROFILE RATED COUNT
            // ==========================================
            const ratedCountSpan = document.getElementById('profile-rated-count');
            if (ratedCountSpan && data.ratedCount !== undefined) {
                ratedCountSpan.textContent = data.ratedCount;
            }

            // ---> USE TRUE BACKEND VALUES FOR INSTANT UI UPDATE <---
            // Robust fallback selector to find the rating container relative to the form or spot ID
            const container = document.querySelector(`.star-rating[data-spot-id="${spotId}"]`) || 
                              form.closest('.spot-card, .modal, .spot-item')?.querySelector('.star-rating');
                              
            if (container && data.newCount !== undefined && data.newAverage !== undefined) {
                container.setAttribute('data-rating', data.newAverage);
                container.setAttribute('data-count', data.newCount);

                // Update text fields if present
                const avgText = container.closest('.spot-card, .modal, .spot-item')?.querySelector('.average-rating-text');
                if (avgText) avgText.textContent = data.newAverage;

                const countText = container.closest('.spot-card, .modal, .spot-item')?.querySelector('.rating-count-text');
                if (countText) countText.textContent = `(${data.newCount})`;

                if (typeof renderSpotRatings === 'function') {
                    renderSpotRatings();
                }
            }

        } else {
            alert(data.message || 'Failed to submit review.');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Failed to submit review.');
    })
    .finally(() => {
        form.removeAttribute('data-submitting');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.style.opacity = "1";
        }
    });
}

// Helper function to update just the rating numbers and stars for that specific spot card
function updateSpotRatingUI(spotId) {
    fetch(`${API_BASE_URL}/api/auth/spot-rating-details?spotId=${spotId}`)
        .then(res => res.json())
        .then(data => {
            const container = document.querySelector(`.star-rating[data-spot-id="${spotId}"]`);
            if (container) {
                container.setAttribute('data-rating', data.averageRating);
                container.setAttribute('data-count', data.totalCount);
                
                if (typeof renderSpotRatings === 'function') {
                    renderSpotRatings();
                }
            }
        })
        .catch(err => {
            console.log("Rating updated successfully");
        });
}

document.addEventListener("DOMContentLoaded", function () {
    const profileBtn = document.getElementById("profileMenuBtn");
    const profileDropdown = document.getElementById("profileDropdown");

    if (profileBtn && profileDropdown) {
        profileBtn.addEventListener("click", function (e) {
            e.preventDefault();
            
            // Check if dropdown is currently closed
            const isClosed = (profileDropdown.style.display === "none" || profileDropdown.style.display === "" || !profileDropdown.style.display);

            if (isClosed) {
                // Open dropdown and highlight profile button with active color
                profileDropdown.style.display = "block";
                profileBtn.classList.add("active");
                profileBtn.style.color = "#f97316"; 
            } else {
                // Close dropdown and reset profile button color to default
                profileDropdown.style.display = "none";
                profileBtn.classList.remove("active");
                profileBtn.style.color = "#000000"; 
            }
        });

        // Close dropdown and reset color when clicking outside
        window.addEventListener("click", function (e) {
            if (!profileBtn.contains(e.target) && !profileDropdown.contains(e.target)) {
                profileDropdown.style.display = "none";
                profileBtn.classList.remove("active");
                profileBtn.style.color = "#000000";
            }
        });
    }
});
document.getElementById("logoutBtn").addEventListener("click", function() {
    fetch('/api/auth/logout', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (response.ok) {
            // Change this to your actual login page URL endpoint
            window.location.href = '/foodspot'; 
        } else {
            alert("Logout failed. Please try again.");
        }
    })
    .catch(error => {
        console.error('Error during logout:', error);
    });
});
// ==========================================
// 1. LOAD EXISTING REVIEW ON PAGE LOAD
// ==========================================
async function loadExistingReview(spotId, form) {
    try {
        const response = await fetch(`${API_BASE_URL}/api/auth/get-user-review?spotId=${spotId}`);
        
        if (response.status === 204 || !response.ok) {
            console.log("No existing review found for this spot.");
            return;
        }

        const reviewData = await response.json();
        console.log("Review data loaded:", reviewData);
        
        // 1. Populate rating input value
        const ratingInput = form.querySelector('.selectedRatingValue') || form.querySelector('#selectedRatingValue');
        if (ratingInput) {
            ratingInput.value = reviewData.ratings;
        }
        
        // 2. Populate textarea comment
        const commentInput = form.querySelector('.review-textarea');
        if (commentInput) {
            commentInput.value = reviewData.review || '';
        }
        
        // 3. Highlight the correct stars using the 'filled' class
        if (reviewData.ratings > 0) {
            const stars = form.querySelectorAll('.star');
            stars.forEach(s => {
                const val = parseInt(s.getAttribute('data-value') || s.getAttribute('data-rating'), 10);
                if (val <= reviewData.ratings) {
                    s.classList.add('filled');
                } else {
                    s.classList.remove('filled');
                }
            });
        }

    } catch (e) {
        console.error("Error loading existing review:", e);
    }
}

// ==========================================
// RENDER SPOT RATINGS & USER COUNTS
// ==========================================
function renderSpotRatings() {
    const starContainers = document.querySelectorAll('.star-rating[data-spot-id]');
    
    starContainers.forEach(container => {
        let averageRating = parseFloat(container.getAttribute('data-rating')) || 0;
        let ratingCount = parseInt(container.getAttribute('data-count')) || 0;
        
        // Limit average rating between 0 and 5
        if (averageRating > 5) averageRating = 5;
        if (averageRating < 0) averageRating = 0;
        
        // 1. Light up the stars (checks both data-value and data-rating for compatibility)
        const stars = container.querySelectorAll('.star');
        stars.forEach(star => {
            const starValue = parseInt(star.getAttribute('data-value') || star.getAttribute('data-rating'), 10);
            if (starValue <= Math.round(averageRating)) {
                star.classList.add('filled');
            } else {
                star.classList.remove('filled');
            }
        });

        // 2. Populate the inner spans (Average rating & count)
        const parentBox = container.closest('.spot-rating-box');
        if (parentBox) {
            const avgSpan = parentBox.querySelector('.avg-score-text');
            const countSpan = parentBox.querySelector('.count-number-text');
            
            if (avgSpan) {
                avgSpan.textContent = `${averageRating}/5`;
            }
            
            if (countSpan) {
                const ratingWord = ratingCount === 1 ? 'rating' : 'ratings';
                countSpan.textContent = `${ratingCount} ${ratingWord}`;
            }
        }
    });
}

// ==========================================
// SINGLE INITIALIZATION ON PAGE LOAD
// ==========================================
document.addEventListener("DOMContentLoaded", () => {
    // 1. Render card ratings and counts
    renderSpotRatings();

    // 2. Load any existing user reviews into active forms
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        const spotIdInput = form.querySelector('input[name="spotId"]');
        const commentInput = form.querySelector('.review-textarea');
        
        if (spotIdInput && spotIdInput.value && commentInput) {
            loadExistingReview(spotIdInput.value, form);
        }
    });
});

// Toggle Save/Bookmark API Call
function toggleSaveSpot(btn) {
    // Prevent the click from bubbling up and triggering card zoom/expansion
    if (event) event.stopPropagation();
    
    const spotId = btn.getAttribute('data-spot-id');
    if (!spotId) return;

    fetch('/api/auth/toggle-save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ spotId: spotId })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to toggle save state');
        }
        return response.json();
    })
    .then(data => {
        // ==========================================
        // LIVE UPDATE PROFILE SAVED COUNT
        // ==========================================
        const savedCountSpan = document.getElementById('profile-saved-count');
        if (savedCountSpan && data.savedCount !== undefined) {
            savedCountSpan.textContent = data.savedCount;
        }

        if (data.saved) {
            // Add orange saved styling
            btn.classList.add('saved');
        } else {
            // Remove saved styling
            btn.classList.remove('saved');
            
            // If the user is currently viewing the "Saved" filter tab, 
            // remove this card immediately from the view so it updates live
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('filter') === 'saved') {
                const card = btn.closest('.food-spot-card');
                if (card) {
                    card.remove();
                    // Optional: Trigger your pagination check if needed
                    if (typeof updatePagination === 'function') {
                        updatePagination();
                    }
                }
            }
        }
    })
    .catch(error => {
        console.error('Error toggling save spot:', error);
    });
}

// Ensure the "Saved" filter button active state syncs correctly with your filter scripts
document.addEventListener("DOMContentLoaded", function () {
    const urlParams = new URLSearchParams(window.location.search);
    const currentFilter = urlParams.get('filter');
    
    if (currentFilter === 'saved') {
        const savedBtn = document.getElementById('saved-spots-btn');
        if (savedBtn) {
            savedBtn.classList.add('active'); // Add your active filter pill class here if you use one
        }
    }
});

