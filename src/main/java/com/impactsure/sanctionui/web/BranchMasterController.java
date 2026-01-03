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

import com.impactsure.sanctionui.entities.BranchMaster;
import com.impactsure.sanctionui.service.impl.BranchService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchMasterController {

    private final BranchService branchService;

    @GetMapping
    public String list(Model model, @RequestParam(value = "id", required = false) Long id) {
        List<BranchMaster> branches = branchService.getAllBranches();
        model.addAttribute("branches", branches);

        BranchMaster form;
        if (id != null) {
            form = branchService.findById(id).orElseGet(BranchMaster::new);
        } else {
            form = new BranchMaster();
        }
        model.addAttribute("branch", form);
        model.addAttribute("active", "branches");
        return "branch-masters";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("branch") BranchMaster branch, Model model) {
        try {
            branchService.save(branch);
            return "redirect:/branches";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("branches", branchService.getAllBranches());
            model.addAttribute("branch", branch);
            model.addAttribute("active", "branches");
            return "branch-masters";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        branchService.deleteById(id);
        return "redirect:/branches";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteBranch(@PathVariable Long id) {
        try {
            branchService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot delete this branch because admissions already exist for it.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to delete the branch.");
        }
    }
}
