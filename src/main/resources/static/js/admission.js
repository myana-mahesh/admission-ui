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
    
    { id: 'absId',          required: true, message: 'ABS ID is required.' },
    { id: 'fullName',       required: true, message: 'Student name is required.' },
    { id: 'dob',            required: true, message: 'Date of birth is required.' },
    { id: 'nationality',    required: true, message: 'Nationality is required.' },
    { id: 'religion',       required: true, message: 'Religion is required.' },
    { id: 'gender',         required: true, message: 'Gender is required.' },
    { id: 'addressLine1',   required: true, message: 'Address is required.' },
    { id: 'pincode',        required: true, pattern: '^[0-9]{6}$', patternMessage: 'Enter a valid 6 digit PIN code.' },
    { id: 'mobile',         required: true, pattern: '^[6-9][0-9]{9}$', patternMessage: 'Enter a valid 10 digit mobile number.' },
    { id: 'email',          required: true, message: 'Enter a valid email address.' },
    { id: 'collegeId',      required: true, message: 'Please select a college.' },
    { id: 'course',         required: true, message: 'Please select a course.' },
    { id: 'dateOfAdmission',required: true, message: 'Date of admission is required.' },
    { id: 'totalFees',      required: true, message: 'Total fees is required.' }
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

    return isValid;
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
        passingYear: $('#hscYear').val(),
        physicsMarks: $('#phyMarks').val(),
        chemistryMarks: $('#chemMarks').val(),
        biologyMarks: $('#bioMarks').val(),
        pcbPercentage: $('#pcbPercentage').val()
      };

      $.ajax({
        url: '/admission/student/create',
        type: 'POST',
        data: JSON.stringify(formData),
        contentType: 'application/json',
        success: async function (response) {
          try {
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
			location.reload();
          } catch (e) {
            console.error(e);
            alert(`Erorr saving data`);
          }
        },
        error: function (xhr, status, error) {
          console.error(error);
          alert('Error: Unable to save student data.');
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
      alert('Missing admission id');
      return;
    }

    $btn.prop('disabled', true).text('Sending...');

    sendAcknowledgementAjax(id)
      .done(function (admission) {
        console.log('Acknowledged:', admission);
        alert('Acknowledgement sent successfully.');
		location.reload();
      })
      .fail(function (xhr) {
        console.error('Error:', xhr.responseText || xhr.statusText);
        alert('Failed to send acknowledgement.');
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

  // gender (🔥 SINGLE VALUE ONLY)
  const genderSelect = document.getElementById("gender");
  if (genderSelect && genderSelect.value) {
    params.append("gender", genderSelect.value);   // FEMALE / MALE
  }

  // redirect
  window.location.href = "/studentlist-filters?" + params.toString();
}