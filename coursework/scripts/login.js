async function login(event) {
    console.log("Login function called");
    event.preventDefault();
  
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
  

    let response = await fetch("http://127.0.0.1:8001/login", {
        method: "GET",
        headers: {
            "Authorization": "Basic " + btoa(username + ":" + password),
        }
    });
    let responseNickname = await fetch("http://127.0.0.1:8001/userinfo", {
        method: "GET",
        headers: {
            "Authorization": "Basic " + btoa(username + ":" + password),
        }
    });
  
    if (response.ok && responseNickname.ok) {
      const data = await responseNickname.json();
      const nickname = data.userNickname; 
      localStorage.setItem("nickname", nickname); // Store the nickname in localStorage
      localStorage.setItem("isLoggedIn", "true");
      localStorage.setItem("username", username); // Store the username in localStorage
      localStorage.setItem("password", password); // Store the password in localStorage
      window.location.href = "observation.html";
    } else {
      document.getElementById("error").innerText = "Invalid credentials!";
    }
  }

  async function register(event) {
    console.log("Register function called");
    event.preventDefault();

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const email = document.getElementById("email").value;
    const userNickname = document.getElementById("nickname").value;

    const userData = {
      username: username,
      password: password,
      email: email,
      userNickname: userNickname
    };

    try {
      let response = await fetch("http://127.0.0.1:8001/registration", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(userData)
      });

      if (response.ok) {
        document.getElementById("success").innerText = "Registration successful!";
        document.getElementById("error").innerText = "";
      } else {
        document.getElementById("success").innerText = "";
        document.getElementById("error").innerText = "Registration failed!";
      }
    } catch (error) {
      console.error("Error during registration:", error);
      document.getElementById("register-error").innerText = "An error occurred!";
    }
  }
  