package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.service.IntegrationMarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/integration/marketplace")
public class IntegrationMarketplaceController {

    @Autowired
    private IntegrationMarketplaceService marketplaceService;

    @GetMapping("/templates")
    public ResultData<List<Map<String, Object>>> getTemplates() {
        return ResultData.success(marketplaceService.getMarketplaceTemplates());
    }

    @PostMapping("/import/{templateId}")
    public ResultData<Void> importTemplate(
            @PathVariable String templateId,
            @RequestParam String connectionId,
            @RequestHeader(value = "X-User-No", required = false) String operator) {
        try {
            marketplaceService.importTemplate(templateId, connectionId, operator);
            return ResultData.success(null, "Template imported successfully");
        } catch (Exception e) {
            return ResultData.fail("Import failed: " + e.getMessage());
        }
    }
}
