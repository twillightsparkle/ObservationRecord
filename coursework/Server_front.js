const express = require('express');
const path = require('path');

const app = express();
const PORT = 3000;

// Serve home.html at the root route
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, '/home.html'));
});

// Serve static files from the current directory
app.use(express.static(__dirname));

app.listen(PORT, () => {
    console.log(`Frontend server running at http://localhost:${PORT}`);
});