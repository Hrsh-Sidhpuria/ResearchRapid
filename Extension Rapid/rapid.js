document.addEventListener('DOMContentLoaded', () => {
    // Load saved notes on popup open
    chrome.storage.local.get(['researchNotes'], function(result) {
        if (result.researchNotes) {
            document.getElementById('notes').value = result.researchNotes;
        }
    });
    
    // Event listeners for actions
    document.getElementById('summBtn').addEventListener('click', summarizeText);
    document.getElementById('suggBtn').addEventListener('click', suggestText);
    document.getElementById('saveNotesBtn').addEventListener('click', saveNotes);
});

// Helper function to pull selection from page DOM
function getSelectedTextFromDOM() {
    return window.getSelection().toString();
}

async function summarizeText() {
    try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (!tab) return;
        
        const [{ result }] = await chrome.scripting.executeScript({
            target: { tabId: tab.id },
            func: getSelectedTextFromDOM 
        });

        if (!result || !result.trim()) {
            showResult('Please select some text first.');
            return;
        }

        showResult('Summarizing text, please wait...');

        const response = await fetch('http://localhost:8080/api/rapid/r/process', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: result, operation: 'summarize' })
        });

        if (!response.ok) {
            throw new Error(`Api Error: ${response.status}`);
        }
        
        const text = await response.text();
        showResult(text);

    } catch (error) {
        console.error(error);
        showResult('An error occurred: ' + error.message);
    }
}

async function suggestText() {
    try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (!tab) return;
        
        const [{ result }] = await chrome.scripting.executeScript({
            target: { tabId: tab.id },
            func: getSelectedTextFromDOM
        });

        if (!result || !result.trim()) {
            showResult('Please select some text first.');
            return;
        }

        showResult('Fetching suggestions, please wait...');

        const response = await fetch('http://localhost:8080/api/rapid/r/process', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: result, operation: 'suggest' }) 
        });

        if (!response.ok) {
            throw new Error(`Api Error: ${response.status}`);
        }
        
        const text = await response.text();
        showResult(text);

    } catch (error) {
        console.error(error);
        showResult('An error occurred: ' + error.message);
    }
}

async function saveNotes() {
    const notesValue = document.getElementById('notes').value;
    chrome.storage.local.set({ researchNotes: notesValue }, () => {
        showResult('<span style="color: green;">Notes saved successfully!</span>');
    });
}

// Formats content and updates the <div id="results"> element
function showResult(content) {
    const resultsDiv = document.getElementById('results');
    if (resultsDiv) {
        // 1. Convert newlines to line breaks
        let formattedContent = content.replace(/\n/g, '<br>');
        
        // 2. Convert **text** into <strong>text</strong> for bold formatting
        formattedContent = formattedContent.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        
        resultsDiv.innerHTML = formattedContent;
    }
}
