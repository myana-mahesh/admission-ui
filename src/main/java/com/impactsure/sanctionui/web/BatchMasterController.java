package com.impactsure.sanctionui.web;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.impactsure.sanctionui.entities.BatchMaster;
import com.impactsure.sanctionui.service.impl.BatchMasterService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/batches")
@RequiredArgsConstructor
public class BatchMasterController {

    private final BatchMasterService batchMasterService;

    @GetMapping
    public String list(Model model, @RequestParam(value = "id", required = false) Long id) {
        List<BatchMaster> batches = batchMasterService.getAllBatches();
        model.addAttribute("batches", batches);

        BatchMaster form = (id != null)
                ? batchMasterService.findById(id).orElseGet(BatchMaster::new)
                : new BatchMaster();
        model.addAttribute("batch", form);
        model.addAttribute("active", "batches");
        return "batch-masters";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("batch") BatchMaster batch, Model model) {
        try {
            batchMasterService.save(batch);
            return "redirect:/batches";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("batches", batchMasterService.getAllBatches());
            model.addAttribute("batch", batch);
            model.addAttribute("active", "batches");
            return "batch-masters";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        batchMasterService.deleteById(id);
        return "redirect:/batches";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteBatch(@PathVariable Long id) {
        try {
            batchMasterService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot delete this batch because admissions already exist for it.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to delete the batch.");
        }
    }
}
