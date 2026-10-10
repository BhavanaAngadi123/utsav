package com.utsav.admin;

import com.utsav.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Audit log writer: uses REQUIRES_NEW so audit records survive rollbacks. */
@Service
public class AuditService {

  private final AuditLogRepository logs;

  public AuditService(AuditLogRepository logs) {
    this.logs = logs;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void log(User actor, String action, String entityType, String entityId, String detail) {
    logs.save(new AuditLog(actor, action, entityType, entityId, detail));
  }

  /**
   * Audit inside the caller's transaction. Use this when the actor was created in the
   * current (still uncommitted) transaction — REQUIRES_NEW runs in a separate
   * transaction that cannot see uncommitted rows, so the actor_id FK would fail.
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void logInTransaction(
      User actor, String action, String entityType, String entityId, String detail) {
    logs.save(new AuditLog(actor, action, entityType, entityId, detail));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void log(String action, String entityType, String entityId, String detail) {
    logs.save(new AuditLog(null, action, entityType, entityId, detail));
  }

  public Page<AuditLog> recent(Pageable pageable) {
    return logs.findAllByOrderByCreatedAtDesc(pageable);
  }
}
