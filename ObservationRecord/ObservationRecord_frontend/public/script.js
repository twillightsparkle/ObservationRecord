document.addEventListener("DOMContentLoaded", function () {
    loadDataRecords();
});

async function loadDataRecords() {
    try {
        let username = localStorage.getItem("username");
        let password = localStorage.getItem("password");
        if (!username || !password) {
            console.error("No credentials found in localStorage.");
            return;
        }
        console.log("Loading data records..."); // Check in dev tools
        let response = await fetch("http://127.0.0.1:8001/datarecord", {
            method: "GET",
            headers: {
                "Authorization": "Basic " + btoa("" + username + ":" + password),
                "Content-Type": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error("Failed to fetch data: " + response.status);
        }

        const data = await response.json();
        console.log(data); // Check in dev tools

        displayObservations(data); 

    } catch (error) {
        console.error("Error fetching records:", error);
    }
}

// display function
function displayObservations(observations) {
    document.getElementById("the-title").innerText = "Observation Record"; 
    console.log(observations); // Check in dev tools
    const container = document.getElementById("observation-details");
    container.innerHTML = ""; // Clear previous content

    observations.forEach(obs => {
        const obsDiv = document.createElement("div");
        obsDiv.classList.add("observation");

        let html = `
            <h3>Name of Observation: ${obs.recordIdentifier}</h3>
            <p><strong>Posted by:</strong> ${obs.recordOwner}</p>
            <p><strong>Description:</strong> ${obs.recordDescription}</p>
            <p><strong>Astronomical Coordinates:</strong></p>
            <ul>
                <li>Declination: ${obs.recordDeclination}</li>
                <li>Right Ascension: ${obs.recordRightAscension}</li>
            </ul>
            <p><strong>Time Received:</strong> ${new Date(obs.recordTimeReceived).toLocaleString()}</p>
            <p><strong>Payload:</strong> ${obs.recordPayload}</p>
        `;
        // Conditionally add "Modified" field if it exists
        if (obs.modified) {
            html += `<p><strong>Last Modified:</strong> ${new Date(obs.modified).toLocaleString()}</p>`;
        }

        // Conditionally add "Update Reason" if it's non-empty
        if (obs.updateReason && obs.updateReason.trim() !== "") {
            html += `<p><strong>Update Reason:</strong> ${obs.updateReason}</p>`;
        }

        html += `<button onclick='addToCollection(${JSON.stringify(obs).replace(/'/g, "\\'")})'>Add to Collection</button>`;

        obsDiv.innerHTML = html;
        container.appendChild(obsDiv);
    });
}

// Call the loader function after the DOM is ready
document.addEventListener("DOMContentLoaded", loadDataRecords);

function toggleFormVisibility(containerId) {
    const container = document.getElementById(containerId);
    container.style.display = container.style.display === "none" ? "block" : "none";
}

function toggleObservationAdd() {
    toggleFormVisibility("observation-form-addobservation-container");
    document.getElementById("observation-form-search-container").style.display = "none"; // Hide search if open
    document.getElementById("printCollectionAsPDF-button").style.display = "none"; // Hide print if open
  }
  
  document.getElementById("observation-form-addobservation").addEventListener("submit", async function (event) {
    event.preventDefault();
    let username = localStorage.getItem("username");
    let password = localStorage.getItem("password");
    if (!username || !password) {
        console.error("No credentials found in localStorage.");
        return;
    }
  
    const data = {
      recordRightAscension: document.getElementById("rightAscension").value,
      recordDeclination: document.getElementById("declination").value,
      recordDescription: document.getElementById("description").value,
      recordIdentifier: document.getElementById("identifier-add").value,
      recordPayload: document.getElementById("payload").value
    };
  
    try {
      const response = await fetch("http://127.0.0.1:8001/datarecord", {
        method: "POST",
        headers: {
            "Authorization": "Basic " + btoa("" + username + ":" + password),
            "Content-Type": "application/json"
        },
        body: JSON.stringify(data)
      });
  
      if (response.ok) {
        document.getElementById("form-response-add").innerText = "Observation submitted successfully";
        document.getElementById("observation-form-addobservation").reset();
        loadDataRecords(); // Reload data records after submission
      } else {
        document.getElementById("form-response-add").innerText = "Submission failed.";
      }
    } catch (err) {
      console.error("Submission error:", err);
      document.getElementById("form-response-add").innerText = "An error occurred.";
    }
  });

function toggleSearchForm() {
    toggleFormVisibility("observation-form-search-container");
    document.getElementById("observation-form-addobservation-container").style.display = "none"; // Hide add form if open
    document.getElementById("printCollectionAsPDF-button").style.display = "none"; // Hide print if open

    document.getElementById("observation-form-search").addEventListener("submit", async function(event) {
        event.preventDefault();
      
        const identifier = document.getElementById("identifier-search").value.trim();
        const author = document.getElementById("Author").value.trim();
        const fromDate = document.getElementById("from-date").value;
        const toDate = document.getElementById("to-date").value;
      
        const params = new URLSearchParams();
      
        if (identifier) params.append("identification", identifier);
        if (author) params.append("nickname", author);
        if (fromDate) params.append("after", new Date(fromDate).toISOString());
        if (toDate) params.append("before", new Date(toDate).toISOString());
      
        try {
            let username = localStorage.getItem("username");
            let password = localStorage.getItem("password");
            if (!username || !password) {
                console.error("No credentials found in localStorage.");
                return;
            }
            console.log("Searching with params:", params.toString()); // Check in dev tools
      
          const response = await fetch(`http://127.0.0.1:8001/search?${params.toString()}`, {
            method: "GET",
            headers: {
              "Authorization": "Basic " + btoa("" + username + ":" + password)
            }
          });
      
          if (response.ok) {
            const data = await response.json();
            displayObservations(data); // Display the search results
            document.getElementById("form-response-search").innerText ="find " + data.length + " results. See below"; // Display the number of records found
          } else {
            document.getElementById("form-response-search").innerText = "Search failed.";
          }
        } catch (error) {
          console.error("Search error:", error);
          document.getElementById("form-response-search").innerText = "An error occurred during search.";
        }
      });
}

// Feature: update observation
async function loadMyObservations() {
    
    const author = localStorage.getItem("nickname");   

    const params = new URLSearchParams();
    if (author) params.append("nickname", author);
    
    try {
      let username = localStorage.getItem("username");
      let password = localStorage.getItem("password");
      if (!username || !password) {
        console.error("No credentials found in localStorage.");
        return;
      }
      console.log("Searching with params:", params.toString()); // Check in dev tools
    
      const response = await fetch(`http://127.0.0.1:8001/search?${params.toString()}`, {
        method: "GET",
        headers: {
          "Authorization": "Basic " + btoa("" + username + ":" + password)
        }
      });
    
      if (response.ok) {
        const data = await response.json();
        console.log(data); // Check in dev tools
        displayMyObservations(data); // Display the search results      
        } 
    } catch (error) {
      console.error("Search error:", error);
      document.getElementById("form-response-search").innerText = "An error occurred during search.";
    }
  }

function displayMyObservations(observations) {
    document.getElementById("the-title").innerText = "My Observations"; 
    const container = document.getElementById("observation-details");
    container.innerHTML = ""; // Clear previous content

    observations.forEach(obs => {
        const obsDiv = document.createElement("div");
        obsDiv.classList.add("observation");

        const formattedTime = new Date(obs.recordTimeReceived).toLocaleString();

        let html = `
            <h3>Name of Observation: ${obs.recordIdentifier}</h3>
            <p><strong>Posted by:</strong> ${obs.recordOwner}</p>
            <p><strong>Description:</strong> ${obs.recordDescription}</p>
            <p><strong>Astronomical Coordinates:</strong></p>
            <ul>
                <li>Declination: ${obs.recordDeclination}</li>
                <li>Right Ascension: ${obs.recordRightAscension}</li>
            </ul>
            <p><strong>Time Received:</strong> ${formattedTime}</p>
            <p><strong>Payload:</strong> ${obs.recordPayload}</p>
        `;
        // Conditionally add "Modified" field if it exists
        if (obs.modified) {
            html += `<p><strong>Last Modified:</strong> ${new Date(obs.modified).toLocaleString()}</p>`;
        }

        // Conditionally add "Update Reason" if it's non-empty
        if (obs.updateReason && obs.updateReason.trim() !== "") {
            html += `<p><strong>Update Reason:</strong> ${obs.updateReason}</p>`;
        }

        html += `
        <div style="margin-top: 10px;">
            <button onclick="showUpdateForm(${obs.id}, ${JSON.stringify(obs).replace(/"/g, '&quot;')})">Update</button>
            <button onclick="deleteObservation(${obs.id})" style="margin-left: 10px; background-color: #f44336; color: white;">Delete</button>
        </div>
        <div id="update-form-${obs.id}" style="display: none; margin-top: 10px; border: 1px solid #ccc; padding: 10px;"></div>
        `;
        obsDiv.innerHTML = html;
        container.appendChild(obsDiv);
    });
}

function showUpdateForm(id, obs) {
    const formDiv = document.getElementById(`update-form-${id}`);
    const isVisible = formDiv.style.display === "block";

    if (isVisible) {
        formDiv.style.display = "none";
        formDiv.innerHTML = "";
    } else {
        formDiv.style.display = "block";

        formDiv.innerHTML = `
            <h4>Update Observation</h4>
            <form onsubmit="submitUpdate(event, ${id})">
                <label>Identifier: <input type="text" name="recordIdentifier" value="${obs.recordIdentifier}" required></label><br>
                <label>Description: <textarea name="recordDescription" required>${obs.recordDescription}</textarea></label><br>
                <label>Right Ascension: <input type="text" name="recordRightAscension" value="${obs.recordRightAscension}" required></label><br>
                <label>Declination: <input type="text" name="recordDeclination" value="${obs.recordDeclination}" required></label><br>
                <label>Payload: <textarea name="recordPayload" required>${obs.recordPayload}</textarea></label><br>
                <label>Update Reason (optional): <textarea name="updateReason">${obs.updateReason}</textarea>}</label><br>
                <button type="submit">Submit Update</button>
            </form>
        `;
    }
}

function submitUpdate(event, id) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);
    const updateData = Object.fromEntries(formData.entries());

    console.log("Update for ID:", id, updateData);

    let username = localStorage.getItem("username");
    let password = localStorage.getItem("password");
    if (!username || !password) {
        console.error("No credentials found in localStorage.");
        return;
    }
    fetch(`http://127.0.0.1:8001/datarecord?id=${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Basic " + btoa("" + username + ":" + password)
      },
      body: JSON.stringify(updateData)
    })
    .then(res => {
        if (!res.ok) throw new Error("Update failed");
        form.closest("div").style.display = "none";
        loadMyObservations(); // Reload the observations after update
    })
    .catch(err => {
        console.error("Update error:", err);
        alert("Failed to update observation.");
    });
}

function deleteObservation(id) {
    if (!confirm("Are you sure you want to delete this observation?")) return;

    let username = localStorage.getItem("username");
    let password = localStorage.getItem("password");
    if (!username || !password) {
        console.error("No credentials found in localStorage.");
        return;
    }
    fetch(`http://127.0.0.1:8001/datarecord?id=${id}`, {
        method: "DELETE",
        headers: {
            "Authorization": "Basic " + btoa("" + username + ":" + password)
        }
    })
    .then(response => {
        if (response.ok) {
            // Refresh the observations list
            loadMyObservations(); // Replace with your actual refresh function
            alert("Observation deleted successfully.");
        } else {
            return response.text().then(text => { throw new Error(text); });
        }
    })
    .catch(err => {
        alert("Failed to delete observation: " + err.message);
    });
}

// Feature: Add to list and print as pdf
let selectedObservations = [];

function addToCollection(observation) {
    if (!selectedObservations.find(o => o.id === observation.id)) {
        selectedObservations.push(observation);
        alert("Added to collection!");
    } else {
        alert("Already in collection.");
    }
}

function removefromCollection(id) {
    selectedObservations = selectedObservations.filter(obs => obs.id !== id);
    displayCollections();
}

function toggleCollections() {
    toggleFormVisibility("printCollectionAsPDF-button");
    document.getElementById("observation-form-addobservation-container").style.display = "none"; // Hide add form if open
    document.getElementById("observation-form-search-container").style.display = "none"; // Hide search if open
    displayCollections();
}

function displayCollections() {
    document.getElementById("the-title").innerText = "My Collection"; 
    const container = document.getElementById("observation-details");
    container.innerHTML = ""; // Clear previous content

    selectedObservations.forEach(obs => {
      console.log(obs); // Check in dev tools
        const obsDiv = document.createElement("div");
        obsDiv.classList.add("observation");

        let html = `
            <h3>Name of Observation: ${obs.recordIdentifier}</h3>
            <p><strong>Posted by:</strong> ${obs.recordOwner}</p>
            <p><strong>Description:</strong> ${obs.recordDescription}</p>
            <p><strong>Astronomical Coordinates:</strong></p>
            <ul>
                <li>Declination: ${obs.recordDeclination}</li>
                <li>Right Ascension: ${obs.recordRightAscension}</li>
            </ul>
            <p><strong>Time Received:</strong> ${new Date(obs.recordTimeReceived).toLocaleString()}</p>
            <p><strong>Payload:</strong> ${obs.recordPayload}</p>
        `;
        // Conditionally add "Modified" field if it exists
        if (obs.modified) {
            html += `<p><strong>Last Modified:</strong> ${new Date(obs.modified).toLocaleString()}</p>`;
        }

        // Conditionally add "Update Reason" if it's non-empty
        if (obs.updateReason && obs.updateReason.trim() !== "") {
            html += `<p><strong>Update Reason:</strong> ${obs.updateReason}</p>`;
        }

        html += `<button onclick='removefromCollection(${obs.id})'>remove</button>`;

        obsDiv.innerHTML = html;
        container.appendChild(obsDiv);
    });
}

function printCollectionAsPDF() {
    const { jsPDF } = window.jspdf;

    if (selectedObservations.length === 0) {
        alert("No observations in your collection to print.");
        return;
    }

    const doc = new jsPDF();
    let y = 10;
    const lineHeight = 8;
    const margin = 10;

    doc.setFont("helvetica");
    doc.setFontSize(12);

    selectedObservations.forEach((obs, index) => {
        if (y > 270) {  // Avoid writing off the page
            doc.addPage();
            y = 10;
        }

        doc.setFont(undefined, "bold");
        doc.text(`Observation ${index + 1}: ${obs.recordIdentifier}`, margin, y);
        y += lineHeight;

        doc.setFont(undefined, "normal");
        doc.text(`Posted by: ${obs.recordOwner}`, margin, y);
        y += lineHeight;
        doc.text(`Description: ${obs.recordDescription}`, margin, y);
        y += lineHeight;

        doc.text(`Declination: ${obs.recordDeclination}`, margin, y);
        y += lineHeight;
        doc.text(`Right Ascension: ${obs.recordRightAscension}`, margin, y);
        y += lineHeight;

        doc.text(`Time Received: ${new Date(obs.recordTimeReceived).toLocaleString()}`, margin, y);
        y += lineHeight;
        doc.text(`Payload: ${obs.recordPayload}`, margin, y);
        y += lineHeight;

        if (obs.modified) {
            doc.text(`Last Modified: ${new Date(obs.modified).toLocaleString()}`, margin, y);
            y += lineHeight;
        }

        if (obs.updateReason && obs.updateReason.trim() !== "") {
            doc.text(`Update Reason: ${obs.updateReason}`, margin, y);
            y += lineHeight;
        }

        y += lineHeight; // Extra space before next entry
    });

    doc.save("observation-collection.pdf");
}



