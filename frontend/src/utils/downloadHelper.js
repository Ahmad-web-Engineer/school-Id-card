/**
 * Robust Blob file downloader for modern browsers (Chrome, Edge, Firefox, Safari).
 *
 * IMPORTANT: Calling window.URL.revokeObjectURL immediately after link.click() causes
 * Chromium to prematurely abort the streaming download of large binary files (e.g., 10+ MB PDFs).
 * This utility uses a 60-second cleanup delay so the browser can finish transferring the file.
 */
export function downloadBlob(blobData, filename) {
  if (!blobData) {
    console.error('downloadBlob: no data provided');
    return;
  }

  const blob = blobData instanceof Blob ? blobData : new Blob([blobData]);
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename || 'download');
  link.style.display = 'none';

  document.body.appendChild(link);
  link.click();

  // Defer DOM removal and URL revocation to ensure the download stream finishes safely
  setTimeout(() => {
    try {
      if (document.body.contains(link)) {
        document.body.removeChild(link);
      }
      window.URL.revokeObjectURL(url);
    } catch (ignored) {}
  }, 60000);
}

export default downloadBlob;
