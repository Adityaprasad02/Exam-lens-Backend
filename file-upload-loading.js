const testUpload = async () => {
  const formData = new FormData();
  const mockFile = new File(['test'], 'test.pdf', { type: 'application/pdf' });
  formData.append('files', mockFile);
  formData.append('req', JSON.stringify({ request: [] }));

  const controller = new AbortController();
  const timeout = setTimeout(() => {
    controller.abort();
    console.error('Request timed out after 20 seconds');
    alert('The server is taking too long to respond. Please try again later.');
  }, 20000);

  try {
    document.body.innerHTML += '<div id="loading">Uploading... Please wait.</div>';
    const response = await fetch('http://localhost:8080/upload-pdf', {
      method: 'POST',
      body: formData,
      signal: controller.signal
    });
    clearTimeout(timeout);
    document.getElementById('loading').remove();
    console.log('Status:', response.status);
    if (!response.ok) {
      alert('Upload failed: ' + response.status);
    }
  } catch (err) {
    clearTimeout(timeout);
    if (document.getElementById('loading')) document.getElementById('loading').remove();
    if (err.name === 'AbortError') {
      // Already handled by timeout
    } else {
      alert('Error: ' + err.message);
    }
  }
};
testUpload();