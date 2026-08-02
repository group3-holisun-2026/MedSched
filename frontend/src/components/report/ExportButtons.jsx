import React, { useState } from 'react';
import Button from '../Button';
import { reportsApi } from '../../api/reports';

// Numele fisierului se construieste aici, nu se citeste din raspuns: SecurityConfig seteaza
// setAllowedHeaders("*") dar NU setExposedHeaders, iar CORS nu expune implicit
// Content-Disposition catre JS — response.headers['content-disposition'] ar fi mereu undefined.
const FILE_NAME_PREFIX = {
  sales: 'raport-vanzari',
  occupancy: 'raport-ocupare',
  'no-show': 'raport-no-show',
};

const EXTENSION = { pdf: 'pdf', xlsx: 'xlsx' };

// Cu responseType: 'blob', un 400/403 vine tot ca Blob, deci error.response.data.message
// e undefined. Corpul trebuie citit ca text si abia apoi parsat.
async function readBlobError(error, fallback) {
  const data = error?.response?.data;
  if (!data) return fallback;

  try {
    const text = typeof data.text === 'function' ? await data.text() : String(data);
    return JSON.parse(text)?.message ?? fallback;
  } catch {
    return fallback;
  }
}

function triggerDownload(blob, fileName) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  // Fara revoke, fiecare export lasa un blob in memorie pana la reincarcarea paginii.
  URL.revokeObjectURL(url);
}

/**
 * Butoanele de export pentru un raport (F-602).
 *
 * @param type    'sales' | 'occupancy' | 'no-show'
 * @param from/to 'yyyy-MM-dd'; cat timp lipsesc, butoanele sunt dezactivate
 * @param onError callback de afisare a erorii (toast-ul difera intre pagini)
 */
export default function ExportButtons({ type, from, to, onError }) {
  // Stare separata per format: altfel un export PDF lent blocheaza si butonul de Excel.
  const [exporting, setExporting] = useState(null);

  const disabled = !from || !to;

  const handleExport = async (format) => {
    if (disabled) {
      onError?.('Selectati intervalul si generati raportul inainte de export.');
      return;
    }

    setExporting(format);
    try {
      const response = await reportsApi.exportReport(type, format, from, to);
      const fileName = `${FILE_NAME_PREFIX[type] ?? 'raport'}_${from}_${to}.${EXTENSION[format]}`;
      triggerDownload(response.data, fileName);
    } catch (error) {
      onError?.(await readBlobError(error, 'Exportul a esuat. Incercati din nou.'));
    } finally {
      setExporting(null);
    }
  };

  return (
    <div className="flex gap-2">
      <Button
        type="button"
        variant="outline"
        onClick={() => handleExport('pdf')}
        disabled={disabled || exporting !== null}
      >
        {exporting === 'pdf' ? 'Se genereaza...' : 'Export PDF'}
      </Button>
      <Button
        type="button"
        variant="outline"
        onClick={() => handleExport('xlsx')}
        disabled={disabled || exporting !== null}
      >
        {exporting === 'xlsx' ? 'Se genereaza...' : 'Export Excel'}
      </Button>
    </div>
  );
}
