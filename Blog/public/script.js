window.addEventListener("load", function() {

	// Replace Date
	let year = new Date().getFullYear();
	let years = document.getElementsByClassName("year");
	for(let i = 0; i < years.length; i++) {
		years[i].innerHTML = year;
	}

	let loaders = document.getElementsByClassName("loader");
	for(let i = 0; i < loaders.length; i++) {
		loadHTML(loaders[i].getAttribute("href"), loaders[i]);
		setInterval(function() {
			loadHTML(loaders[i].getAttribute("href"), loaders[i]);
		}, 1000);
	}

	// Set Navigation
	let buttons = document.getElementsByTagName("nav")[0].getElementsByTagName("a");
	for(let i = 0; i < buttons.length; i++) {
		let button = buttons[i];
		if(button.href === window.location.href) {
			button.classList.add("active");
		}
	}

});

// Loader
function loadHTML(href, element) {
	var request = new XMLHttpRequest();
	request.open("GET", href, true);
	request.send(null);
	request.onload = function(e) {
		if(request.readyState === 4) {
			if(request.status === 200) {
				element.innerHTML = request.responseText;
			}
		}
	}
}

// Copy to Clipboard
function copy(element, text) {
	let temporary = element.getAttribute("switch");
	element.setAttribute("switch", element.innerHTML);
	element.innerHTML = temporary;
	if(element.classList.contains("active")) {
		element.classList.remove("active");
	} else {
		let pseudo = document.createElement("input");
		pseudo.value = text;
		document.body.appendChild(pseudo);
		pseudo.select();
		document.execCommand("copy");
		pseudo.parentElement.removeChild(pseudo);
		element.classList.add("active");
	}
}
