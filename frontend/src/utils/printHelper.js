import api from '../services/api';

/**
 * Direct Print a PDF or Image from an API URL using a hidden iframe.
 * Triggers the browser/system print dialog directly without requiring file downloads.
 */
export async function directPrintUrl(endpointUrl) {
  try {
    const res = await api.get(endpointUrl, { responseType: 'blob' });
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/pdf' });
    const contentType = blob.type || 'application/pdf';
    const blobUrl = window.URL.createObjectURL(blob);

    if (contentType.startsWith('image/')) {
      // For images: create an iframe with a clean printable document containing the image
      return new Promise((resolve) => {
        const printFrame = document.createElement('iframe');
        printFrame.style.position = 'fixed';
        printFrame.style.right = '0';
        printFrame.style.bottom = '0';
        printFrame.style.width = '0';
        printFrame.style.height = '0';
        printFrame.style.border = '0';

        document.body.appendChild(printFrame);

        const frameDoc = printFrame.contentWindow.document;
        frameDoc.open();
        frameDoc.write(`
          <!DOCTYPE html>
          <html>
            <head>
              <title>Print Card</title>
              <style>
                @page { margin: 0; size: auto; }
                body { margin: 0; padding: 0; display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #fff; }
                img { max-width: 100%; max-height: 100vh; object-fit: contain; }
              </style>
            </head>
            <body>
              <img src="${blobUrl}" onload="window.focus(); window.print();" />
            </body>
          </html>
        `);
        frameDoc.close();

        // Cleanup after printing
        setTimeout(() => {
          try {
            if (document.body.contains(printFrame)) {
              document.body.removeChild(printFrame);
            }
            window.URL.revokeObjectURL(blobUrl);
          } catch (ignored) {}
          resolve(true);
        }, 60000);
      });
    }

    // For PDFs: load blob directly into iframe and trigger print
    return new Promise((resolve) => {
      const printFrame = document.createElement('iframe');
      printFrame.style.position = 'fixed';
      printFrame.style.right = '0';
      printFrame.style.bottom = '0';
      printFrame.style.width = '0';
      printFrame.style.height = '0';
      printFrame.style.border = '0';
      printFrame.src = blobUrl;

      printFrame.onload = () => {
        try {
          printFrame.contentWindow.focus();
          printFrame.contentWindow.print();
        } catch (err) {
          // Fallback to popup if iframe printing is blocked
          const win = window.open(blobUrl, '_blank');
          if (win) {
            win.onload = () => win.print();
          }
        }
        resolve(true);

        setTimeout(() => {
          try {
            if (document.body.contains(printFrame)) {
              document.body.removeChild(printFrame);
            }
            window.URL.revokeObjectURL(blobUrl);
          } catch (ignored) {}
        }, 60000);
      };

      document.body.appendChild(printFrame);
    });
  } catch (error) {
    console.error('Direct print failed:', error);
    throw error;
  }
}
