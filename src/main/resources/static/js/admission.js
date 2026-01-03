(function initReceiptHoverPreview() {
  const selector = 'input[type="file"][data-field="receipt"]';

  function bind(input) {
    if (input.dataset.previewBound === "1") return;
    input.dataset.previewBound = "1";

    const wrap = input.closest('.file-wrap');
    if (!wrap) return;

    const preview = wrap.querySelector('.file-preview');
    if (!preview) return;

    let objUrl = null;

    input.addEventListener('change', () => {
      const f = input.files && input.files[0];

      wrap.classList.remove('has-file');
      preview.innerHTML = '';
      if (objUrl) URL.revokeObjectURL(objUrl);
      objUrl = null;

      if (!f) return;

      wrap.classList.add('has-file');
      objUrl = URL.createObjectURL(f);

      const isImage = (f.type || '').startsWith('image/');
      const isPdf = (f.type || '') === 'application/pdf' || f.name.toLowerCase().endsWith('.pdf');

      preview.innerHTML = `
        <div class="fp-name">${f.name}</div>
        ${isImage ? `<img class="fp-img" src="${objUrl}" alt="preview" />` : ''}
        <div class="fp-meta">${(f.size/1024).toFixed(1)} KB • ${f.type || 'file'}</div>
        ${isPdf ? `<a class="btn btn-sm btn-dark mt-2 w-100" href="${objUrl}" target="_blank" rel="noopener">View PDF</a>` : ''}
      `;
    });
  }

  // bind existing
  document.querySelectorAll(selector).forEach(bind);

  // bind future (if rows added dynamically)
  const mo = new MutationObserver(() => {
    document.querySelectorAll(selector).forEach(bind);
  });
  mo.observe(document.body, { childList: true, subtree: true });
})();

$(document).ready(function () {
	
	$("body").on("change","#discountRemark",function(){
		if($(this).val()=="Other"){
			$("#discountRemarkOtherDiv").removeClass("d-none")
		}else{
			$("#discountRemarkOtherDiv").addClass("d-none")
		}
	})
  $("#discountRemark").trigger("change")

  // ================== COLLEGE -> COURSE FILTER + SEAT INFO ==================
  const collegeSelect = document.getElementById('collegeId');
  const courseSelect = document.getElementById('course');
  const seatInfo = document.getElementById('seatInfo');
  let courseSeatMap = new Map();

  function renderSeatInfo(courseId) {
    if (!seatInfo) return;
    const info = courseSeatMap.get(String(courseId || ''));
    if (!info) {
      seatInfo.textContent = '';
      return;
    }
    seatInfo.textContent =
      `Remaining: ${info.remainingSeats} · On Hold: ${info.onHoldSeats} · Utilized: ${info.utilizedSeats} · Total: ${info.totalSeats}`;
  }

  function setCourseOptions(items, selectedId) {
    if (!courseSelect) return;
    courseSelect.innerHTML = '<option value=""></option>';
    items.forEach(item => {
      const opt = document.createElement('option');
      opt.value = item.courseId;
      const label = item.courseCode ? `${item.courseCode} - ${item.courseName}` : item.courseName;
      opt.textContent = label || '';
      courseSelect.appendChild(opt);
    });
    if (selectedId) {
      courseSelect.value = String(selectedId);
    }
    renderSeatInfo(courseSelect.value);
  }

  function loadCollegeCourses(collegeId, selectedCourseId) {
    if (!collegeId) {
      courseSeatMap = new Map();
      setCourseOptions([], null);
      return;
    }
    fetch(`/college-courses?collegeId=${encodeURIComponent(collegeId)}`)
      .then(res => res.ok ? res.json() : [])
      .then(items => {
        courseSeatMap = new Map();
        (items || []).forEach(item => {
          courseSeatMap.set(String(item.courseId), item);
        });
        setCourseOptions(items || [], selectedCourseId);
      })
      .catch(() => {
        courseSeatMap = new Map();
        setCourseOptions([], null);
      });
  }

  if (collegeSelect) {
    const initialCourseId = courseSelect ? courseSelect.value : null;
    if (collegeSelect.value) {
      loadCollegeCourses(collegeSelect.value, initialCourseId);
    }
    collegeSelect.addEventListener('change', () => {
      loadCollegeCourses(collegeSelect.value, null);
    });
  }

  if (courseSelect) {
    courseSelect.addEventListener('change', () => {
      renderSeatInfo(courseSelect.value);
    });
  }


  // ================== OTHER DOCUMENTS DYNAMIC ADD ==================
  let otherDocCounter = document.querySelectorAll('#otherDocsContainer .other-doc').length || 0;

  $('#addOtherDocBtn').on('click', function () {
    otherDocCounter++;
    const code = 'OTHERS' + otherDocCounter;  // e.g. OTHERS1, OTHERS2...

    const block = `
      <div class="col-md-6 other-doc">
        <input type="text"
               class="form-control mb-1 other-doc-label"
               placeholder="Enter document name (e.g. Bonafide Certificate)" />

        <input type="file"
               class="form-control other-doc-file"
               name="docOther${otherDocCounter}"
               accept=".pdf,.jpg,.png"
               data-doc-type="${code}" />
      </div>
    `;

    $('#otherDocsContainer').append(block);
  });

  document.getElementById('addInstallmentBtn')
    ?.addEventListener('click', () => window.addInstallment());

  const togglePartialBtn = document.getElementById('togglePartialPaymentBtn');
  const partialForm = document.getElementById('partialPaymentForm');
  const partialSubmit = document.getElementById('partialPaymentSubmit');
  const partialError = document.getElementById('partialPaymentError');
  const partialModalEl = document.getElementById('partialPaymentModal');
  const partialModal = partialModalEl && window.bootstrap ? new bootstrap.Modal(partialModalEl) : null;
  const historyModalEl = document.getElementById('installmentHistoryModal');
  const historyModal = historyModalEl && window.bootstrap ? new bootstrap.Modal(historyModalEl) : null;
  const historyBody = document.getElementById('installmentHistoryBody');
  const roleInput = document.getElementById('role');
  const currentRole = roleInput ? roleInput.value : null;
  let activeInstallmentId = null;

  function setPartialError(message) {
    if (!partialError) return;
    partialError.textContent = message || '';
    partialError.classList.toggle('d-none', !message);
  }

  if (togglePartialBtn) {
    togglePartialBtn.addEventListener('click', () => {
      setPartialError('');
      if (partialModal) {
        partialModal.show();
      }
    });
  }

  if (partialForm && partialSubmit) {
    partialSubmit.addEventListener('click', async () => {
      setPartialError('');

      const amount = document.getElementById('partialAmount')?.value;
      const mode = document.getElementById('partialMode')?.value;
      if (!amount || Number(amount) <= 0) {
        setPartialError('Enter a valid payment amount.');
        return;
      }
      if (!mode) {
        setPartialError('Select a payment mode.');
        return;
      }

      const formData = new FormData();
      const admissionId = partialForm.querySelector('input[name="admissionId"]')?.value;
      const role = partialForm.querySelector('input[name="role"]')?.value;
      const txnRef = document.getElementById('partialTxnRef')?.value || '';
      const receiptInput = document.getElementById('partialReceipt');
      const receiptFile = receiptInput?.files?.[0];

      if (admissionId) formData.append('admissionId', admissionId);
      if (role) formData.append('role', role);
      formData.append('amount', amount);
      formData.append('mode', mode);
      formData.append('txnRef', txnRef);
      if (receiptFile) {
        formData.append('receipt', receiptFile);
      }
      try {
        const res = await fetch('/admission/installments/partial-payment', {
          method: 'POST',
          body: formData
        });
        if (!res.ok) {
          const msg = await res.text();
          setPartialError(msg || 'Failed to save payment.');
          return;
        }
        if (partialModal) {
          partialModal.hide();
        }
        window.location.reload();
      } catch (err) {
        setPartialError('Failed to save payment.');
      }
    });
  }

  function renderHistoryRows(rows) {
    if (!historyBody) return;
    const showVerify = currentRole === 'HO';
    const emptyColspan = showVerify ? 11 : 10;
    if (!rows || rows.length === 0) {
      historyBody.innerHTML = `<tr><td colspan="${emptyColspan}" class="text-center text-muted py-3">No payments found.</td></tr>`;
      return;
    }
    const dtFormatter = new Intl.DateTimeFormat(undefined, {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
    historyBody.innerHTML = rows.map(row => {
      const receipt = row.receiptUrl
        ? `<a href="${row.receiptUrl}" target="_blank" class="text-decoration-none">${row.receiptName || 'Receipt'}</a>`
        : '<span class="text-muted">-</span>';
      const invoice = row.invoiceUrl
        ? `<a href="${row.invoiceUrl}" target="_blank" class="text-decoration-none">${row.invoiceNumber || 'Invoice'}</a>`
        : '<span class="text-muted">-</span>';
      const paidOn = row.paidOn || '-';
      const status = row.status || '-';
      const verifiedBy = row.verifiedBy || '-';
      let verifiedAt = '-';
      if (row.verifiedAt) {
        const parsed = new Date(row.verifiedAt);
        if (!Number.isNaN(parsed.getTime())) {
          verifiedAt = dtFormatter.format(parsed);
        }
      }
      const canVerify = showVerify && row.verified === false;
      const verifyBtn = canVerify
        ? `<button type="button" class="btn btn-sm btn-primary-theme verify-payment-btn" data-payment-id="${row.paymentId}">Verify</button>`
        : '<span class="text-muted">-</span>';
      return `
        <tr>
          <td>${paidOn}</td>
          <td>${row.amount ?? '-'}</td>
          <td>${row.paymentMode || '-'}</td>
          <td>${row.txnRef || '-'}</td>
          <td>${row.receivedBy || '-'}</td>
          <td>${status}</td>
          <td>${verifiedBy}</td>
          <td>${verifiedAt}</td>
          <td>${receipt}</td>
          <td>${invoice}</td>
          ${showVerify ? `<td>${verifyBtn}</td>` : ''}
        </tr>
      `;
    }).join('');
  }

  document.body.addEventListener('click', async (event) => {
    const btn = event.target.closest('.installment-history-btn');
    if (!btn) return;
    const installmentId = btn.getAttribute('data-installment-id');
    if (!installmentId) return;
    activeInstallmentId = installmentId;
    if (historyBody) {
      const showVerify = currentRole === 'HO';
      const loadingColspan = showVerify ? 11 : 10;
      historyBody.innerHTML = `<tr><td colspan="${loadingColspan}" class="text-center text-muted py-3">Loading...</td></tr>`;
    }
    if (historyModal) {
      historyModal.show();
    }
    try {
        const res = await fetch(`/admission/installments/${installmentId}/payments`);
        if (!res.ok) {
          renderHistoryRows([]);
          return;
        }
        const data = await res.json();
        renderHistoryRows(Array.isArray(data) ? data : []);
    } catch (err) {
      renderHistoryRows([]);
    }
  });

  document.body.addEventListener('click', async (event) => {
    const btn = event.target.closest('.verify-payment-btn');
    if (!btn) return;
    const paymentId = btn.getAttribute('data-payment-id');
    if (!paymentId) return;
    btn.disabled = true;
    try {
      const res = await fetch(`/admission/installments/payments/${paymentId}/verify`, { method: 'POST' });
      if (res.ok) {
        if (activeInstallmentId) {
          const refreshed = await fetch(`/admission/installments/${activeInstallmentId}/payments`);
          if (refreshed.ok) {
            const data = await refreshed.json();
            renderHistoryRows(Array.isArray(data) ? data : []);
          }
        }
        window.location.reload();
      } else {
        btn.disabled = false;
      }
    } catch (err) {
      btn.disabled = false;
    }
  });

  // ================== DOC TYPE MAP ==================
  const DOC_TYPE_MAP = {
    "10th marksheet": "SSC10",
    "12th marksheet": "HSC12",
    "leaving certificate / transfer certificate": "LC",
    "migration certificate": "MIG",
    "aadhaar card": "AADHAAR",
    "passport size photo": "PHOTO",
    "other document": "OTHERS",
    "other": "OTHERS"
  };

  // ===================================================
  //           FRONT-END VALIDATION HELPERS
  // ===================================================

  const fieldConfigs = [
    
    { id: 'fullName',       required: true, message: 'Student name is required.' },
    { id: 'dob',            required: true, message: 'Date of birth is required.' },
    { id: 'nationality',    required: true, message: 'Nationality is required.' },
    { id: 'religion',       required: true, message: 'Religion is required.' },
    { id: 'gender',         required: true, message: 'Gender is required.' },
    { id: 'addressLine1',   required: true, message: 'Address is required.' },
    { id: 'pincode',        required: true, pattern: '^[0-9]{6}$', patternMessage: 'Enter a valid 6 digit PIN code.' },
    { id: 'mobile',         required: true, pattern: '^[6-9][0-9]{9}$', patternMessage: 'Enter a valid 10 digit mobile number.' },
    { id: 'email',          required: true, message: 'Enter a valid email address.' },
    { id: 'batch',          required: true, message: 'Batch is required.' },
    { id: 'collegeId',      required: true, message: 'Please select a college.' },
    { id: 'course',         required: true, message: 'Please select a course.' },
    { id: 'dateOfAdmission',required: true, message: 'Date of admission is required.' },
    { id: 'totalFees',      required: true, message: 'Total fees is required.' },
    { id: 'hscCollege',     required: true, message: 'HSC college name is required.' },
    { id: 'hscSubjects',    required: true, message: 'HSC subjects are required.' },
    { id: 'hscYear',        required: true, message: 'HSC passing year is required.' },
    { id: 'phyMarks',       required: true, message: 'Physics marks are required.' },
    { id: 'chemMarks',      required: true, message: 'Chemistry marks are required.' },
    { id: 'bioMarks',       required: true, message: 'Biology marks are required.' },
    { id: 'hscPercentage',  required: true, message: 'HSC percentage is required.' }
  ];

  function setInvalid(el, msg) {
    if (!el) return;
    el.classList.add('is-invalid');
    const wrapper = el.closest('.form-floating') || el.parentElement;
    const feedback = wrapper ? wrapper.querySelector('.invalid-feedback') : null;
    if (feedback && msg) {
      feedback.textContent = msg;
    }
  }

  function clearInvalid(el) {
    if (!el) return;
    el.classList.remove('is-invalid');
  }

  // Clear error on input/change
  fieldConfigs.forEach(cfg => {
    const el = document.getElementById(cfg.id);
    if (!el) return;
    el.addEventListener('input', () => clearInvalid(el));
    el.addEventListener('change', () => clearInvalid(el));
  });

  function validateAdmissionForm() {
    let isValid = true;

    fieldConfigs.forEach(cfg => {
      const el = document.getElementById(cfg.id);
      if (!el) return;

      clearInvalid(el);
      const value = (el.value || '').trim();

      if (cfg.required && !value) {
        setInvalid(el, cfg.message);
        isValid = false;
        return;
      }

      if (cfg.pattern && value) {
        const re = new RegExp(cfg.pattern);
        if (!re.test(value)) {
          setInvalid(el, cfg.patternMessage || cfg.message);
          isValid = false;
        }
      }

      if (['phyMarks', 'chemMarks', 'bioMarks', 'hscPercentage'].includes(cfg.id) && value) {
        const num = parseFloat(value);
        if (isNaN(num) || num < 0 || num > 100) {
          setInvalid(el, 'Value must be between 0 and 100.');
          isValid = false;
        }
      }

      // additional numeric check for courseYears
      if (cfg.id === 'courseYears' && value) {
        const num = parseInt(value, 10);
        if (isNaN(num) || num < 1) {
          setInvalid(el, cfg.message);
          isValid = false;
        }
      }
    });

    // Extra check: Actual fees ≤ Total fees
    const totalFeesEl = document.getElementById('totalFees');
    const actualFeesEl = document.getElementById('actualFees');
    if (totalFeesEl && actualFeesEl) {
      const total = parseFloat(totalFeesEl.value || '0');
      const actual = parseFloat(actualFeesEl.value || '0');
      clearInvalid(actualFeesEl);
      if (!isNaN(total) && !isNaN(actual) && actual > total) {
        setInvalid(actualFeesEl, 'Actual fees cannot be greater than total fees.');
        isValid = false;
      }
    }

    const otherPaymentFields = document.querySelectorAll('[data-other-payment-field="true"]');
    otherPaymentFields.forEach(group => {
      const required = group.dataset.required === 'true';
      if (!required) return;
      const inputType = group.dataset.inputType;
      const inputs = group.querySelectorAll('.other-payment-input');
      let hasValue = false;

      if (inputType === 'checkbox') {
        hasValue = Array.from(inputs).some(input => input.checked);
      } else if (inputType === 'radio') {
        hasValue = Array.from(inputs).some(input => input.checked);
      } else if (inputType === 'select') {
        const select = inputs[0];
        hasValue = !!(select && select.value);
      } else {
        const input = inputs[0];
        hasValue = !!(input && (input.value || '').trim());
      }

      const firstInput = inputs[0];
      if (firstInput) {
        clearInvalid(firstInput);
      }
      if (!hasValue && firstInput) {
        setInvalid(firstInput, 'This field is required.');
        isValid = false;
      }
    });

    return isValid;
  }

  function buildOtherPaymentsPayload() {
    const payload = [];
    const groups = document.querySelectorAll('[data-other-payment-field="true"]');
    groups.forEach(group => {
      const fieldId = Number(group.dataset.fieldId);
      if (!fieldId) return;
      const inputType = group.dataset.inputType;
      const inputs = group.querySelectorAll('.other-payment-input');
      const entries = [];

      if (inputType === 'checkbox') {
        inputs.forEach(input => {
          if (!input.checked) return;
          const optionId = Number(input.dataset.optionId || '');
          const value = input.dataset.optionValue || input.value || '';
          entries.push({ optionId: optionId || null, value });
        });
      } else if (inputType === 'radio') {
        const selected = Array.from(inputs).find(input => input.checked);
        if (selected) {
          const optionId = Number(selected.dataset.optionId || '');
          const value = selected.dataset.optionValue || selected.value || '';
          entries.push({ optionId: optionId || null, value });
        }
      } else if (inputType === 'select') {
        const select = inputs[0];
        if (select && select.value) {
          const optionId = Number(select.value || '');
          const selectedOption = select.options[select.selectedIndex];
          const value = selectedOption?.dataset?.optionValue || selectedOption?.text || '';
          entries.push({ optionId: optionId || null, value });
        }
      } else {
        const input = inputs[0];
        if (input && (input.value || '').trim()) {
          entries.push({ optionId: null, value: input.value.trim() });
        }
      }

      payload.push({ fieldId, entries });
    });

    return payload;
  }

  // ===================================================
  //           INSTALLMENTS & UPLOAD METADATA
  // ===================================================

  function buildInstallmentUpserts() {
    const rows = document.querySelectorAll('#installmentsBody tr');
    const upserts = [];

    rows.forEach((tr, idx) => {
      const srNoCell = tr.querySelector('td:first-child');
      const installmentNo = Number(srNoCell?.textContent?.trim()) || (idx + 1);

      const amountEl      = tr.querySelector('input[data-field="amount"]');
      const dateEl        = tr.querySelector('input[data-field="date"]');
      const modeEl        = tr.querySelector('select[data-field="mode"]');
      const receivedByEl  = tr.querySelector('input[data-field="receivedBy"]');
      const statusEl      = tr.querySelector('select[data-field="status"]');
	  const txnRefEl      = tr.querySelector('input[data-field="txnRef"]');

      const amount     = Number(amountEl?.value ?? '');
      const dueDate    = dateEl?.value || null;
      const mode       = modeEl?.value || null;
      const status     = statusEl?.value || null;
	  const txnRef     = txnRefEl?.value || null;
      let receivedBy = receivedByEl?.value || null;

      const hasAny = (isFinite(amount) && amount > 0) || !!dueDate || !!mode;
      if (!hasAny) return;
	  debugger
	  if((status && status=='Paid') && (!receivedBy || receivedBy=="")){
		receivedBy = userName
	  }

      upserts.push({
        id: null,
        studyYear: tr.dataset.year,
        tempId: `inst-${installmentNo}`,
        installmentNo,
        amountDue: isFinite(amount) ? amount : null,
        dueDate,
        mode,
        receivedBy,
        status,
		txnRef,
        yearlyFees: yearBudgets.get(Number.parseInt(tr.dataset.year))
      });
    });

    return upserts;
  }

  function buildReceiptUploadsFromInstallmentsTable() {
    const rows = document.querySelectorAll('#installmentsBody tr');
    const metaFiles = [];
    const filesToSend = [];

    rows.forEach((tr, idx) => {
      const srNoCell = tr.querySelector('td:first-child');
      const installmentNo = Number(srNoCell?.textContent?.trim()) || (idx + 1);

      const fileInput = tr.querySelector('input[type="file"][data-field="receipt"]');
      if (fileInput?.files?.length) {
        const f = fileInput.files[0];
        metaFiles.push({
          docTypeCode: 'RECEIPT',
          filename: f.name,
          mimeType: f.type || 'application/octet-stream',
          sizeBytes: f.size,
          installmentTempId: `inst-${installmentNo}`,
          installmentId: null
        });
        filesToSend.push(f);
      }
    });

    return { metaFiles, filesToSend };
  }

  function getUploadPairs() {
    const sections = [...document.querySelectorAll(".section")];
    const uploadSection = sections.find(sec => {
      const t = sec.querySelector(".title h5");
      return t && t.textContent.trim().toLowerCase() === "upload documents";
    });
    if (!uploadSection) return [];

    return [...uploadSection.querySelectorAll('input[type="file"]')].map(inp => {
      const labelEl = inp.closest(".col-md-6")?.querySelector(".form-label");
      const label = (labelEl?.textContent || "").trim().toLowerCase();
      return { input: inp, label };
    });
  }

  function buildGeneralDocUploads() {
    const uploadSection = document.querySelector(".uploadsection");
    if (!uploadSection) return { metaFiles: [], filesToSend: [] };

    const metaFiles = [];
    const filesToSend = [];

    // Fixed docs
    uploadSection.querySelectorAll('.col-md-6:not(.other-doc) input[type="file"]').forEach(inp => {
      const f = inp.files?.[0];
      if (!f) return;

      const labelEl = inp.closest(".col-md-6")?.querySelector(".form-label");
      const label = (labelEl?.textContent || "").trim().toLowerCase();

      const key = Object.keys(DOC_TYPE_MAP).find(k => label.startsWith(k));
      const docTypeCode = key ? DOC_TYPE_MAP[key] : null;

      metaFiles.push({
        docTypeCode,
        filename: f.name,
        mimeType: f.type || "application/octet-stream",
        sizeBytes: f.size,
        installmentTempId: null,
        installmentId: null,
        label: null
      });
      filesToSend.push(f);
    });

    // Other docs
    uploadSection.querySelectorAll('.other-doc').forEach(col => {
      const fileInput  = col.querySelector('input.other-doc-file');
      const labelInput = col.querySelector('input.other-doc-label');

      const f = fileInput?.files?.[0];
      if (!f) return;

      const customLabel = labelInput?.value?.trim() || null;
      const docTypeCode = fileInput.dataset.docType || null;

      metaFiles.push({
        docTypeCode,
        filename: f.name,
        mimeType: f.type || "application/octet-stream",
        sizeBytes: f.size,
        installmentTempId: null,
        installmentId: null,
        label: customLabel
      });
      filesToSend.push(f);
    });

    return { metaFiles, filesToSend };
  }

  async function postUploads(admissionId) {
    const installments = buildInstallmentUpserts();
    const { metaFiles: receiptMeta, filesToSend: receiptFiles } = buildReceiptUploadsFromInstallmentsTable();
    const { metaFiles: docMeta, filesToSend: docFiles } = buildGeneralDocUploads();

    const metaFiles = [...receiptMeta, ...docMeta];
    const filesToSend = [...receiptFiles, ...docFiles];

    if (filesToSend.length === 0 && metaFiles.length === 0 && installments.length === 0) {
      throw new Error("Nothing to upload.");
    }

    if (filesToSend.length !== metaFiles.length) {
      throw new Error(`metadata.files (${metaFiles.length}) must match files count (${filesToSend.length})`);
    }

    const fd = new FormData();
    const metadata = { files: metaFiles, installments };

    fd.append("metadata", new Blob([JSON.stringify(metadata)], { type: "application/json" }));
    filesToSend.forEach((f) => fd.append("files", f, f.name));

    const res = await fetch(`/admission/${admissionId}/uploads`, {
      method: "POST",
      body: fd
    });
    if (!res.ok) {
      const text = await res.text().catch(() => "");
      throw new Error(`Upload failed (${res.status}): ${text}`);
    }
	
    return res.json();
  }

  // ===================================================
  //              SAVE ADMISSION (CREATE/UPDATE)
  // ===================================================

  function showAdmissionSaveModal(status, title, message, onClose) {
    const modalEl = document.getElementById('admissionSaveStatusModal');
    const titleEl = document.getElementById('admissionSaveStatusTitle');
    const alertEl = document.getElementById('admissionSaveStatusAlert');
    const descEl = document.getElementById('admissionSaveStatusDesc');

    if (!modalEl || !titleEl || !alertEl || !descEl || !window.bootstrap) {
      if (status === 'success') {
        alert(title + (message ? `\n${message}` : ''));
      } else {
        alert((title || 'Error') + (message ? `\n${message}` : ''));
      }
      if (typeof onClose === 'function') onClose();
      return;
    }

    titleEl.textContent = title || 'Save Status';
    alertEl.className = `alert mb-3 ${status === 'success' ? 'alert-success' : 'alert-danger'}`;
    alertEl.textContent = status === 'success' ? 'Saved successfully.' : 'Save failed.';
    descEl.textContent = message || '';

    if (modalEl.parentElement !== document.body) {
      document.body.appendChild(modalEl);
    }

    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    const handleHidden = () => {
      modalEl.removeEventListener('hidden.bs.modal', handleHidden);
      document.body.classList.remove('modal-open');
      document.querySelectorAll('.modal-backdrop').forEach((el) => el.remove());
      if (typeof onClose === 'function') onClose();
    };
    modalEl.addEventListener('hidden.bs.modal', handleHidden);
    modal.show();
  }

  function formatBackendError(raw) {
    const text = (raw || '').toString();
    if (!text) return null;
    if (/blood_group/i.test(text)) {
      return 'Invalid blood group.';
    }
    if (/aadhaar|aadhar/i.test(text)) {
      return 'Invalid Aadhaar number.';
    }
    if (/data truncation/i.test(text)) {
      return 'Some fields are too long. Please shorten the input.';
    }
    return null;
  }

  function getSelectedPerkIds() {
    return $(".student-perk-checkbox:checked")
      .map(function () { return parseInt($(this).val(), 10); })
      .get();
  }

  function saveStudentPerks(studentId) {
    debugger
    const perkIds = getSelectedPerkIds();
    if (!studentId || !$(".student-perk-checkbox").length) {
      return Promise.resolve();
    }
    return $.ajax({
      url: "/student/" + studentId + "/perks",
      type: "PUT",
      data: JSON.stringify(perkIds),
      contentType: "application/json"
    });
  }

  $("body").on("click", "#submit_admission_form", function () {
    // 1) run front-end validations
    if (!validateAdmissionForm()) {
      // scroll to first invalid field (nice UX)
      const firstInvalid = document.querySelector('.is-invalid');
      if (firstInvalid) {
        firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      return;
    }

    let formData = {};

    let actualFees = $("#actualFees").val();
    let installmentsSum = $("#installmentsSum").val();

    if (actualFees && installmentsSum) {
      try {
        actualFees = Number.parseFloat(actualFees);
        installmentsSum = Number.parseFloat(installmentsSum);
      } catch (err) {
        console.log(err);
      }
    }

    if (actualFees && installmentsSum && installmentsSum === actualFees) {
      $('.student-details').each(function () {
        formData[this.id] = $(this).val();
      });
      formData['formDate']=new Date().toISOString().split('T')[0];

      // ✅ Attach SSC details explicitly
      formData.sscDetails = {
        percentage: $('#sscPercentage').val(),
        board: $('#sscBoard').val(),
        passingYear: $('#sscYear').val(),
        registrationNumber: $('#sscRegNo').val()
      };

      formData.hscDetails = {
        collegeName: $('#hscCollege').val(),
        subjects: $('#hscSubjects').val(),
        registrationNumber: $('#hscRegNo').val(),
        passingYear: $('#hscYear').val(),
        physicsMarks: $('#phyMarks').val(),
        chemistryMarks: $('#chemMarks').val(),
        biologyMarks: $('#bioMarks').val(),
        pcbPercentage: $('#pcbPercentage').val(),
        percentage: $('#hscPercentage').val()
      };
      formData.otherPayments = buildOtherPaymentsPayload();

      $.ajax({
        url: '/admission/student/create',
        type: 'POST',
        data: JSON.stringify(formData),
        contentType: 'application/json',
        success: async function (response) {
          try {
            const studentId = $("#studentId").val()
              || response?.student?.studentId
              || response?.studentId;
            try {
              await saveStudentPerks(studentId);
            } catch (e) {
              console.warn("Failed to save perks:", e);
            }
            const result = await postUploads(response.admissionId);
            console.log("✅ Uploaded metadata:", result);
            $("#installmentsBody tr").each(function () {
              const $row = $(this);
              const status = $row.find("select[data-field='status']").val();
              const installmentId = $row.find("input[name$='.installmentId']").val();

              if (status === "Paid" && installmentId) {
                // generateInvoiceForRow($row);
              }
            });
            showAdmissionSaveModal('success', 'Admission Saved', 'Your changes were saved successfully.', function () {
              window.location.href = `/admissions?id=${response.admissionId}`;
            });
          } catch (e) {
            console.error(e);
            const msg = e && e.message ? e.message : 'Upload failed. Please try again.';
            showAdmissionSaveModal('error', 'Save Failed', msg);
          }
        },
        error: function (xhr, status, error) {
          const rawMsg = (xhr && xhr.responseJSON && xhr.responseJSON.message)
            ? xhr.responseJSON.message
            : (xhr && xhr.responseText ? xhr.responseText : 'Unable to save student data.');
          const friendly = formatBackendError(rawMsg);
          const msg = (friendly || rawMsg || '').toString().trim() || 'Unable to save student data.';
          console.error(error || msg);
          showAdmissionSaveModal('error', 'Save Failed', msg);
        }
      });
    } else if (!actualFees || actualFees === 0) {
      alert('Error: Invalid Actual Fees');
    } else if (!installmentsSum || installmentsSum === 0) {
      alert('Error: Invalid Installment Sum');
    } else if (actualFees !== installmentsSum) {
      alert('Error: Installment sum should match Actual Fees after discount');
    }
  });

  // ===================================================
  //              SEND ADMISSION ACKNOWLEDGEMENT
  // ===================================================

  $("body").on("click", "#submitBtn", function (e) {
    // also validate before sending acknowledgement
    if (!validateAdmissionForm()) {
      const firstInvalid = document.querySelector('.is-invalid');
      if (firstInvalid) {
        firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      return;
    }

    const $btn = $(this);
    const id = $btn.data('id');
    if (!id) {
      showResponseModal('error', 'Missing admission id.');
      return;
    }

    $btn.prop('disabled', true).text('Sending...');

    sendAcknowledgementAjax(id)
      .done(function (admission) {
        console.log('Acknowledged:', admission);
        showResponseModal('success', 'Acknowledgement sent successfully.', window.location.href);
      })
      .fail(function (xhr) {
        console.error('Error:', xhr.responseText || xhr.statusText);
        const msg = xhr?.responseText || 'Failed to send acknowledgement.';
        showResponseModal('error', msg);
      })
      .always(function () {
        $btn.prop('disabled', false).text('Send Acknowledgement');
      });
  });

  function sendAcknowledgementAjax(admissionId) {
    return $.ajax({
      url: '/admission/send-acknowledgement',
      method: 'POST',
      data: { id: admissionId },
      dataType: 'json'
    });
  }

  // ===================================================
  //              BRANCH APPROVE FOR HO
  // ===================================================
  $("body").on("click", "#branchApproveBtn", function () {
    const $btn = $(this);
    const id = $btn.data('id');
    if (!id) {
      showResponseModal('error', 'Missing admission id.');
      return;
    }
    const originalHtml = $btn.html();
    $btn.prop('disabled', true).text('Approving...');
    $.ajax({
      url: '/admission/branch-approve',
      method: 'POST',
      data: { id: id }
    })
      .done(function () {
        showResponseModal('success', 'Admission approved for HO.', window.location.href);
      })
      .fail(function (xhr) {
        console.error('Branch approval failed:', xhr.responseText || xhr.statusText);
        const msg = xhr?.responseText || 'Failed to approve admission.';
        showResponseModal('error', msg);
      })
      .always(function () {
        $btn.prop('disabled', false).html(originalHtml);
      });
  });

});

$("body").on("click", "#confirmCancelBtn", function (e) {
  submitCancelAdmission();
});

$("body").on("click", "#confirmCancelAdmissionBtn", function (e) {
  submitCancelAdmission("confirmCancelAdmissionBtn");
});


function submitCancelAdmission(buttonId) {

	
  let charges = $("#cancelCharges").val();
  let remarks = $("#remarks_admission_cancel").val();
  let handlingPerson = ""
	debugger
  if(buttonId!=="confirmCancelAdmissionBtn"){
	// ✅ simple mandatory validation
	  if (!charges || charges.trim() === "") {
	    alert("Cancel Charges is required");
	    $("#cancelCharges").focus();
	    return;
	  }

	  if (!remarks || remarks.trim() === "") {
	    alert("Remarks is required");
	    $("#remarks_admission_cancel").focus();
	    return;
	  }
  }else{
	charges = $("input[name='cancelCharges']").val()
	remarks =  $("textarea[name='remark']").val()
	handlingPerson = $("input[name='handlingPerson']").val();
	
  }
  
  const requestData = {
    admissionId: $("#admissionId").val(),
    cancelCharges: charges,
    remark: remarks,
    handlingPerson: handlingPerson,
    refundProofFileName: "",
    role : $("#role").val()
  };


  $.ajax({
    url: "/admission/cancel-admission",
    type: "POST",
    contentType: "application/json",
    data: JSON.stringify(requestData),
    success: function (response) {
      showResponseModal(
          "success",
          response || "Admission cancelled successfully!",
          "/admissionlist"
      );
    },
    error: function (xhr, status, error) {
      showResponseModal(
          "error",
          xhr.responseText || "Something went wrong!"
      );
      console.error("AJAX Error:", error);
    }
  });
}


function showResponseModal(type, message, redirectUrl) {
  const modalEl = document.getElementById("responseModal");
  const modal = new bootstrap.Modal(modalEl);

  const title = document.getElementById("responseModalTitle");
  const msg = document.getElementById("responseModalMessage");
  const icon = document.getElementById("responseModalIcon");

  msg.textContent = message;

  if (type === "success") {
    title.textContent = "Success";
    icon.className = "bi bi-check-circle-fill text-success";
  } else {
    title.textContent = "Error";
    icon.className = "bi bi-x-circle-fill text-danger";
  }

  modal.show();

  // Redirect after modal close
  if (redirectUrl) {
    modalEl.addEventListener(
        "hidden.bs.modal",
        function () {
          window.location.href = redirectUrl;
        },
        { once: true }
    );
  }
}


$("body").on("click", "#updateCancellationBtn", function () {
  saveCancellationDetails();
});

function saveCancellationDetails() {
  const formData = new FormData();

  formData.append("admissionId", $("#admissionId").val());
  formData.append("cancelCharges", $("input[name='cancelCharges']").val());
  formData.append("handlingPerson", $("input[name='handlingPerson']").val());
  formData.append("remark", $("textarea[name='remark']").val());

  // Refund Proof
  const refundInput = $("input[name='refundProof']")[0];
  if (refundInput && refundInput.files.length > 0) {
    const refundFile = refundInput.files[0];
    formData.append("refundProof", refundFile);                 // ✅ FILE
    formData.append("refundProofFileName", refundFile.name);    // ✅ NAME
  }else{
    formData.append("refundProofFileName",$("#refundProofText").text());
  }

  // Student Acknowledgement Proof
  const acknowledgementInput = $("input[name='studentAcknowledgementProof']")[0];
  if (acknowledgementInput && acknowledgementInput.files.length > 0) {
    const acknowledgementFile = acknowledgementInput.files[0];
    formData.append("studentAcknowledgementProof", acknowledgementFile);                 // ✅ FILE
    formData.append("studentAcknowledgementProofFileName", acknowledgementFile.name);    // ✅ NAME
  }else{
    formData.append("studentAcknowledgementProofFileName",$("#studentAcknowledgementProofText").text());
  }
  $.ajax({
    url: "/admission/save-cancellation-details",
    type: "POST",
    data: formData,
    processData: false,
    contentType: false,
    success: function (response) {
      showResponseModal(
          "success",
          response ,
          "/admissions?id=" + $("#admissionId").val()
      );
    },
    error: function (xhr) {
     // alert("Error saving cancellation details");
      showResponseModal(
          "error",
          "Error saving cancellation details" ,
          "/admissions?id=" + $("#admissionId").val()
      );
      console.error(xhr);
    }
  });
}



const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

$("body").on("change", "input[type='file'][name='docFiles']", function () {

  const file = this.files[0];

  if (file && file.size > MAX_FILE_SIZE) {

    alert(
        "Upload Error!\n\n" +
        file.name + " exceeds the maximum allowed size.\n" +
        "Max file size per document is 10 MB."
    );

    // reset file input
    $(this).val("");
  }
});

$("body").on("change", ".doc-radio", function () {
debugger
  const admissionId = $("#admissionId").val();
  if (!admissionId) {
    alert("Please save the admission before verifying documents.");
    return;
  }
  const documentCode = $(this).attr("name")
      .replace("docType[", "")
      .replace("]", "");
  const receivedType = $(this).val();

  $.post("/admission/document-verification-save", {
    admissionId: admissionId,
    documentCode: documentCode,
    receivedType: receivedType
  });
});



$("body").on("click", ".verify-btn", function () {

  const admissionId = $("#admissionId").val();
  if (!admissionId) {
    alert("Please save the admission before verifying documents.");
    return;
  }
  const documentCode = $(this).data("doc");
  const card = $(this).closest(".checklist-item");

  $.post("/admission/document-verification-verify", {
    admissionId: admissionId,
    documentCode: documentCode
  }, function () {
    card.removeClass("pending").addClass("verified");
    card.find(".form-check-label").removeClass("text-danger")
        .addClass("text-success");
  });
});

$("body").on("click", "#collegeVerifyBtn", function () {
  const admissionId = $("#admissionId").val();
  if (!admissionId) {
    alert("Missing admission id");
    return;
  }
  $.post("/admission/college-verification-verify", { admissionId: admissionId }, function () {
    location.reload();
  });
});

$("body").on("click", "#collegeRejectBtn", function () {
  const admissionId = $("#admissionId").val();
  if (!admissionId) {
    alert("Missing admission id");
    return;
  }
  $.post("/admission/college-verification-reject", { admissionId: admissionId }, function () {
    location.reload();
  });
});


function onPercentageChange(el) {
  const value = parseFloat(el.value);

  if (isNaN(value) || value < 0 || value > 100) {
    alert("❌ Percentage must be between 0 and 100");
    el.value = "";
    el.focus();
  }
}


function onYearChange(el) {
  const year = parseInt(el.value);
  const currentYear = new Date().getFullYear();

  if (isNaN(year) || year < 1950 || year > currentYear) {
    alert("❌ Enter a valid passing year (1950 - " + currentYear + ")");
    el.value = "";
    el.focus();
  }
}


  function onMarksChange() {

  let phy = Number($('#phyMarks').val());
  let chem = Number($('#chemMarks').val());
  let bio = Number($('#bioMarks').val());

  if ([phy, chem, bio].some(m => m < 0 || m > 100)) {
  alert("Marks must be between 0 and 100");
  return;
}

  if (phy && chem && bio) {
  let pcb = ((phy + chem + bio) / 300) * 100;
  $('#pcbPercentage').val(pcb.toFixed(2));
}
}

  function onHscYearChange(input) {
  const year = Number(input.value);
  const currentYear = new Date().getFullYear();

  if (year < 1950 || year > currentYear) {
  alert("Enter a valid HSC passing year");
  input.value = '';
}
}


function calculateAge() {
  const dobInput = document.getElementById('dob').value;
  const ageInput = document.getElementById('age');

  if (!dobInput) {
    ageInput.value = '';
    return;
  }

  const dob = new Date(dobInput);
  const today = new Date();

  let age = today.getFullYear() - dob.getFullYear();
  const monthDiff = today.getMonth() - dob.getMonth();
  const dayDiff = today.getDate() - dob.getDate();

  // Adjust if birthday not yet occurred this year
  if (monthDiff < 0 || (monthDiff === 0 && dayDiff < 0)) {
    age--;
  }

  ageInput.value = age;
}

document.addEventListener('DOMContentLoaded', () => {
  const dobInput = document.getElementById('dob');
  const ageInput = document.getElementById('age');
  if (!dobInput || !ageInput) {
    return;
  }
  dobInput.addEventListener('input', calculateAge);
  dobInput.addEventListener('change', calculateAge);
  calculateAge();
});


function applyFilters() {

  const params = new URLSearchParams();

  // pagination (keep current or reset)
  params.append("page", 0);
  params.append("size", document.getElementById("paginationSize")?.value || 10);

  // search text
  const q = document.querySelector("input[name='q']")?.value;
  if (q && q.trim() !== "") {
    params.append("q", q.trim());
  }

  // batch (single)
  const batch = document.getElementById("batch")?.value;
  if (batch && batch !== "") {
    params.append("batch", batch);
  }

  const collegeId = document.getElementById("collegeId")?.value;
  if (collegeId && collegeId !== "") {
    params.append("collegeId", collegeId);
  }

  const courseId = document.getElementById("courseId")?.value;
  if (courseId && courseId !== "") {
    params.append("courseId", courseId);
  }

  const admissionYearId = document.getElementById("admissionYearId")?.value;
  if (admissionYearId && admissionYearId !== "") {
    params.append("admissionYearId", admissionYearId);
  }

  const perkId = document.getElementById("perkId")?.value;
  if (perkId && perkId !== "") {
    params.append("perkId", perkId);
  }

  // gender (🔥 SINGLE VALUE ONLY)
  const genderSelect = document.getElementById("gender");
  if (genderSelect && genderSelect.value) {
    params.append("gender", genderSelect.value);   // FEMALE / MALE
  }

  // redirect
  window.location.href = "/studentlist-filters?" + params.toString();
}
