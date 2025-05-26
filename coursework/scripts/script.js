// load data records when the page is loaded
let sortBase = "views"; // Default sort base
document.addEventListener("DOMContentLoaded", function () {
    sortRecords(sortBase);
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
// display full observation function
async function viewFullObservation(obs) {
    try {
        let username = localStorage.getItem("username");
        let password = localStorage.getItem("password");
        if (!username || !password) {
            console.error("No credentials found in localStorage.");
            return;
        }
        console.log("Loading data records..."); // Check in dev tools
        let response = await fetch(`http://127.0.0.1:8001/view/?id=${obs.id}`, {
            method: "GET",
            headers: {
                "Authorization": "Basic " + btoa("" + username + ":" + password),
                "Content-Type": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error("Failed to fetch data: " + response.status);
        }
    } catch (error) {
        console.error("Error fetching records:", error);
    }

    getComments(obs.id); // Fetch comments for the observation
    const container = document.getElementById("observation-details");
    container.innerHTML = ""; // Clear everything for full view

    const fullView = document.createElement("div");
    fullView.classList.add("full-observation");

    fullView.innerHTML = `
        <h2>${obs.recordIdentifier}</h2>
        <p><strong>Posted by:</strong> ${obs.recordOwner}</p>
        <p style="white-space: pre-wrap; word-wrap: break-word;"><strong>Description:</strong> ${obs.recordDescription}</p>
        <p><strong>Astronomical Coordinates:</strong></p>
        <ul>
            <li>Declination: ${obs.recordDeclination}</li>
            <li>Right Ascension: ${obs.recordRightAscension}</li>
        </ul>
        <p><strong>Time Received:</strong> ${new Date(obs.recordTimeReceived).toLocaleString()}</p>
        <p><strong>Payload:</strong></p>
        <pre style="white-space: pre-wrap; word-wrap: break-word;">${obs.recordPayload}</pre>
        <p><strong>Views: </strong> ${obs.view}</p>
        <p><strong>Rating: </strong> ${obs.averageRating >= 0 ? obs.averageRating : "No rating yet"}</p>
        ${obs.modified ? `<p><strong>Last Modified:</strong> ${new Date(obs.modified).toLocaleString()}</p>` : ""}
        ${obs.updateReason?.trim() ? `<p><strong>Update Reason:</strong> ${obs.updateReason}</p>` : ""}
        <input type="number" id="rating" min="1" max="5" />
        <button onclick="rateObservation(${obs.id}, document.getElementById('rating').value)">Rate</button>

        <br>

        <textarea id="comment" placeholder="Leave your comment here..."></textarea>
        <button onclick="commentObservation(${obs.id}, document.getElementById('comment').value)">Comment</button>

        <button onclick="addToCollection(${JSON.stringify(obs).replace(/"/g, '&quot;')})">Add to Collection</button>
        <button onclick='
            if (document.getElementById("sort-options").style.display == "block") {
                sortRecords(sortBase);
            } else {
                loadDataRecords();
            }
        '>Back to List</button>
        <h3>Comments:</h3>
        <div id="comments-section" style="margin-top: 20px;"> </div>
    `;

    container.appendChild(fullView);
    fullView.scrollIntoView({ behavior: "smooth", block: "start" });
}

// display function
function displayObservations(observations) {
    document.getElementById("the-title").innerText = "Observation Record"; 
    console.log(observations); // Check in dev tools
    const container = document.getElementById("observation-details");
    container.innerHTML = ""; // Clear previous content

    observations.forEach(obs => {
        const obsDiv = document.createElement("div");
        obsDiv.id = `observation-${obs.id}`;
        obsDiv.classList.add("observation");

        // Show only summary
        obsDiv.innerHTML = `
            <h3>Name of Observation: ${obs.recordIdentifier}</h3>
            <p><strong>Posted by:</strong> ${obs.recordOwner}</p>
            <p><strong>Description:</strong> ${obs.recordDescription}</p>
            <button onclick="viewFullObservation(${JSON.stringify(obs).replace(/"/g, '&quot;')})">View Details</button>
            <div id="full-details-${obs.id}" style="display: none;"></div>
        `;

        container.appendChild(obsDiv);
    });
}


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
        document.getElementById("sort-options").style.display = "none"; // Hide sort options
      
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
            <p style="white-space: pre-wrap; word-wrap: break-word;"><strong>Description:</strong> ${obs.recordDescription}</p>
            <p><strong>Astronomical Coordinates:</strong></p>
            <ul>
            <li>Declination: ${obs.recordDeclination}</li>
            <li>Right Ascension: ${obs.recordRightAscension}</li>
            </ul>
            <p><strong>Time Received:</strong> ${formattedTime}</p>
            <div id="payload-container-${obs.id}" style="display: none;">
            <p><strong>Payload:</strong></p>
            <pre style="white-space: pre-wrap; word-wrap: break-word;">${obs.recordPayload}</pre>
            </div>
            <button onclick="togglePayload(${obs.id})" id="toggle-payload-button-${obs.id}">View Payload</button>
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
// Support function for My Observations field
function togglePayload(id) {
            const payloadContainer = document.getElementById(`payload-container-${id}`);
            const toggleButton = document.getElementById(`toggle-payload-button-${id}`);
            if (payloadContainer.style.display === "none") {
            payloadContainer.style.display = "block";
            toggleButton.innerText = "Hide Payload";
            } else {
            payloadContainer.style.display = "none";
            toggleButton.innerText = "View Payload";
            }
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
                <label>Update Reason (optional): <textarea name="updateReason">${obs.updateReason? obs.updateReason:""}</textarea></label><br>
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
    document.getElementById("sort-options").style.display = "none";
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
            <p style="white-space: pre-wrap; word-wrap: break-word;"><strong>Description:</strong> ${obs.recordDescription}</p>
            <p><strong>Astronomical Coordinates:</strong></p>
            <ul>
                <li>Declination: ${obs.recordDeclination}</li>
                <li>Right Ascension: ${obs.recordRightAscension}</li>
            </ul>
            <p><strong>Time Received:</strong> ${new Date(obs.recordTimeReceived).toLocaleString()}</p>
            <p><strong>Payload:</strong></p>
            <pre style="white-space: pre-wrap; word-wrap: break-word;">${obs.recordPayload}</pre>
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

    const doc = new jsPDF({
        orientation: "portrait",
        unit: "mm",
        format: "a4",
        putOnlyUsedFonts: true,
    });

    let y = 10;
    const lineHeight = 6;
    const margin = 10;

    // Use a built-in monospace font
    doc.setFont("courier", "normal");
    doc.setFontSize(10);

    selectedObservations.forEach((obs, index) => {
        if (y > 270) {
            doc.addPage();
            y = 10;
        }

        doc.text(`Observation ${index + 1}: ${obs.recordIdentifier}`, margin, y);
        y += lineHeight;

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

        // Print Payload exactly, line by line
        doc.text(`Payload:`, margin, y);
        y += lineHeight;

        const payloadLines = (obs.recordPayload || "").split("\n");

        payloadLines.forEach(line => {
            const wrappedLines = doc.splitTextToSize(line, 190); // 190mm = page width - 2*margin
            wrappedLines.forEach(wrappedLine => {
                if (y > 270) {
                    doc.addPage();
                    y = 10;
                }
                doc.text(wrappedLine, margin, y, { baseline: "top" });
                y += lineHeight;
            });
        });

        y += lineHeight;

        if (obs.modified) {
            doc.text(`Last Modified: ${new Date(obs.modified).toLocaleString()}`, margin, y);
            y += lineHeight;
        }

        if (obs.updateReason && obs.updateReason.trim() !== "") {
            doc.text(`Update Reason: ${obs.updateReason}`, margin, y);
            y += lineHeight;
        }

        y += lineHeight;
    });

    doc.save("observation-collection.pdf");
}



// Feature: Rating and commenting
async function rateObservation(obsId, rating) {
    const username = localStorage.getItem("username");
    const password = localStorage.getItem("password");

    if (!username || !password) {
        console.error("No credentials found in localStorage.");
        alert("Please log in to rate.");
        return;
    }

    const authHeader = "Basic " + btoa(username + ":" + password);

    try {
        const response = await fetch(`http://127.0.0.1:8001/rating`, {
            method: "POST",
            headers: {
                "Authorization": authHeader
            },
            body: JSON.stringify({
                id: obsId,
                rating: rating
            })
        });

        if (!response.ok) {
            throw new Error(`Rating failed with status ${response.status}`);
        }

        console.log("Rating submitted successfully.");
        alert("Rating submitted!");
    } catch (error) {
        console.error("Rating error:", error);
        alert("Failed to submit rating.");
    }
}

async function commentObservation(obsId, comment) {
    const username = localStorage.getItem("username");
    const password = localStorage.getItem("password");

    if (!username || !password) {
        console.error("No credentials found in localStorage.");
        alert("Please log in to comment.");
        return;
    }

    if (!comment || comment.trim().length === 0) {
        alert("Comment cannot be empty.");
        return;
    }

    const authHeader = "Basic " + btoa(username + ":" + password);

    try {
        const response = await fetch("http://127.0.0.1:8001/comment", {
            method: "POST",
            headers: {
                "Authorization": authHeader,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                id: obsId,
                comment: comment
            })
        });

        if (!response.ok) {
            throw new Error(`Comment failed with status ${response.status}`);
        }

        console.log("Comment submitted successfully.");
        alert("Comment submitted!");
    } catch (error) {
        console.error("Comment error:", error);
        alert("Failed to submit comment.");
    }
}

async function getComments(obsId) {
    const username = localStorage.getItem("username");
    const password = localStorage.getItem("password");
    if (!username || !password) {
        console.error("No credentials found in localStorage.");
        alert("Please log in to view comments.");
        return;
    }
    const authHeader = "Basic " + btoa(username + ":" + password);
    try {
        const response = await fetch(`http://127.0.0.1:8001/comments?id=${obsId}`, {
            method: "GET",
            headers: {
                "Authorization": authHeader
            }
        });
        // Example Response: from database
        // Response Messages: [{  
        //     "recordId": 1,
        //     "commentId": 6,
        //     "commentText": "ssss",
        //     "commentTime": "2025-05-13T14:18:13.519Z",
        //     "userNickname": "3xt"
        // }]
        if (!response.ok) {
            throw new Error(`Fetching comments failed with status ${response.status}`);
        }
        const comments = await response.json();
        console.log("Comments fetched successfully:", comments);
        displayComments(comments);
    } catch (error) {
        document.getElementById("comments-section").innerHTML = "<p>No comments available.</p>";
        console.error("Error fetching comments:", error);
    }
}

function displayComments(comments) {
    const container = document.getElementById("comments-section");
    container.innerHTML = ""; // Clear previous comments
    comments.forEach(comment => {
        const commentDiv = document.createElement("div");
        commentDiv.classList.add("comment");
        commentDiv.innerHTML = `
            <p style="white-space: pre-wrap; word-wrap: break-word;"><strong>${comment.userNickname}:</strong> ${comment.commentText}</p>
            <p><em>${new Date(comment.commentTime).toLocaleString()}</em></p>
        `;
        container.appendChild(commentDiv);
    });
}

// Feature: sorting records for homepage

async function sortRecords(base) {
    const validBases = ["views", "ratings", "comments", "random"];
    if (!validBases.includes(base)) {
        console.error("Invalid sort base:", base);
        return;
    }
    sortBase = base; // Update the global variable

    try {
        let username = localStorage.getItem("username");
        let password = localStorage.getItem("password");
        if (!username || !password) {
            console.error("No credentials found in localStorage.");
            return;
        }

        const response = await fetch(`http://127.0.0.1:8001/toprecords?base=${base}`, {
            method: "GET",
            headers: {
                "Authorization": "Basic " + btoa(username + ":" + password)
            }
        });

        if (!response.ok) {
            throw new Error(`Sorting failed with status ${response.status}`);
        }

        const data = await response.json();
        console.log(`Records sorted by ${base}:`, data);
        displayObservations(data); // Display the sorted records
    } catch (error) {
        console.error("Error sorting records:", error);
    }
}
function sortCurrentBase() {
    sortRecords(sortBase);
}



