import api from '../services/api';

/**
 * Direct Print a PDF or Image from an API URL using a hidden iframe with fallback to print window.
 * Triggers the browser/system print dialog directly without requiring file downloads.
 */
export async function directPrintUrl(endpointUrl, docTitle = 'Print Document') {
  try {
    // Prevent accidental double "/api/v1" prefixing when baseURL is already "/api/v1"
    const cleanUrl = endpointUrl.startsWith('/api/v1')
      ? endpointUrl.substring('/api/v1'.length)
      : endpointUrl;

    const res = await api.get(cleanUrl, {
      responseType: 'blob',
      timeout: 180000, // 3 minutes timeout for heavy imposition jobs
    });

    const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/pdf' });
    const contentType = blob.type || 'application/pdf';
    const blobUrl = window.URL.createObjectURL(blob);

    if (contentType.startsWith('image/')) {
      return printImageBlob(blobUrl, docTitle);
    } else {
      return printPdfBlob(blobUrl, docTitle);
    }
  } catch (error) {
    console.error('Direct print failed:', error);
    throw error;
  }
}

function printImageBlob(blobUrl, docTitle) {
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
          <title>${docTitle || 'Print Document'}</title>
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

function printPdfBlob(blobUrl, docTitle) {
  return new Promise((resolve) => {
    const printFrame = document.createElement('iframe');
    printFrame.style.position = 'fixed';
    printFrame.style.right = '0';
    printFrame.style.bottom = '0';
    printFrame.style.width = '0';
    printFrame.style.height = '0';
    printFrame.style.border = '0';
    printFrame.src = blobUrl;
    printFrame.title = docTitle || 'Print Document';

    let resolved = false;
    const finish = () => {
      if (!resolved) {
        resolved = true;
        resolve(true);
      }
    };

    const tryPrint = () => {
      try {
        if (printFrame.contentWindow) {
          printFrame.contentWindow.focus();
          printFrame.contentWindow.print();
          finish();
          return;
        }
      } catch (err) {
        console.warn('Iframe print access restricted, attempting popup print:', err);
      }

      // Safe fallback for browsers blocking cross-document PDF iframe printing
      try {
        const printWindow = window.open(blobUrl, '_blank');
        if (printWindow) {
          printWindow.focus();
          setTimeout(() => {
            try { printWindow.print(); } catch (e) {}
          }, 1000);
        }
      } catch (e) {
        console.warn('Popup print fallback warning:', e);
      }
      finish();
    };

    printFrame.onload = () => {
      setTimeout(tryPrint, 400);
    };

    // Safety timeout in case iframe onload does not fire for embedded PDF plugin
    setTimeout(() => {
      if (!resolved) {
        tryPrint();
      }
    }, 2000);

    document.body.appendChild(printFrame);

    setTimeout(() => {
      try {
        if (document.body.contains(printFrame)) {
          document.body.removeChild(printFrame);
        }
        window.URL.revokeObjectURL(blobUrl);
      } catch (ignored) {}
    }, 120000);
  });
}
