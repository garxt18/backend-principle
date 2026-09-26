package com.backendprinciple.playground.dsa;

import com.backendprinciple.playground.common.security.AuthUser;
import com.backendprinciple.playground.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dsa")
public class DsaController {

    private final DsaService dsa;

    public DsaController(DsaService dsa) {
        this.dsa = dsa;
    }

    public record UpdateBody(Boolean solved, Boolean revision, @Size(max = 10_000) String notes) {
    }

    @GetMapping("/sheet")
    public DsaService.Sheet sheet(@CurrentUser AuthUser me) {
        return dsa.sheet(me.id());
    }

    @PutMapping("/problems/{id}")
    public DsaService.ProblemState update(@CurrentUser AuthUser me, @PathVariable Long id,
                                          @Valid @RequestBody UpdateBody body) {
        return dsa.update(me.id(), id, new DsaService.UpdateRequest(body.solved(), body.revision(), body.notes()));
    }
}
