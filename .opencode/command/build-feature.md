---
description: Build a feature using test-driven orchestrator workflow. Checks backlog.md, gathers requirements, creates plan, executes with specialized agents.
argument-hint: <feature-description>
model: github-copilot/claude-opus-4.5
tools:
  write: true
  read: true
  bash: true
  grep: true
  glob : true
  list: true
  webfetch: true
---

# Build Feature Command

Trigger orchestrator to build a feature using test-driven workflow with specialized agents.

## Workflow

**User provides:** Feature description in $ARGUMENTS

**Orchestrator executes:**

1. **Check backlog.md** - Look for existing feature notes in `.opencode/backlog.md`
2. **Gather requirements** - Ask clarifying questions if needed
3. **Create plan** - Write implementation plan with agent assignments
4. **Get approval** - Wait for user confirmation
5. **Execute TDD workflow** - Delegate to specialized agents in test-first order

## Orchestrator Instructions

You are orchestrating a feature build. Follow this process:

### Step 1: Check Existing Context

```bash
# Check backlog.md for feature information
cat .opencode/backlog.md
```

Look for:
- Feature description
- Requirements notes
- Technical decisions
- Open questions

### Step 2: Gather Requirements

If information is missing or unclear, ask targeted questions:

**Functional:**
- User story (As [user], I want [capability], so that [benefit])
- Main use cases
- Edge cases
- Error handling

**Technical:**
- Components involved (backend services, frontend pages)
- Database changes needed
- API endpoints required
- Security requirements (auth, validation, rate limiting)

**Examples:**
- "Should password reset tokens expire? If so, how long?"
- "What happens if user requests multiple resets?"
- "Any rate limiting needed (e.g., 3 requests per hour)?"

### Step 3: Create Implementation Plan

Write a clear plan with:

```markdown
# Feature: [Name]

## Requirements Summary
- [Key requirement 1]
- [Key requirement 2]
- [Key requirement 3]

## Technical Approach

### Backend
- Endpoints: [list with methods]
- Services: [list]
- Entities: [list]
- Database: [migrations needed]
- Security: [auth, validation, rate limiting]

### Frontend (if applicable)
- Pages: [list]
- Components: [list]
- API integration: [endpoints to call]

## Implementation Workflow (Test-Driven)

### Phase 1: Tests First
**Agent:** java-testing-agent
**Task:** Create comprehensive test suite covering all requirements
**Deliverable:** Tests that define the contract (will fail initially)

### Phase 2: Backend Implementation
**Agent:** java-backend-agent
**Task:** Implement backend to make tests pass
**Deliverable:** Working backend, all tests passing

### Phase 3: Frontend Implementation (if applicable)
**Agent:** sveltekit-frontend-agent
**Task:** Build UI components and pages using shadcn
**Deliverable:** Working frontend with type safety

### Phase 4: Additional Tasks (if needed)
**Agent:** email-composer-agent / github-actions-agent
**Task:** [Specific task]
**Deliverable:** [Specific output]

## Success Criteria
✅ All tests passing (>90% coverage)
✅ Build successful
✅ Type checks passing (frontend)
✅ Security requirements met
✅ Feature works as specified

## Estimated Effort
[Your time estimate]
```

### Step 4: Get Approval

Present plan clearly:

```
---
🎯 FEATURE PLAN READY

[Show plan summary here]

❓ APPROVAL REQUIRED

Review the plan above. Does it:
- ✅ Capture all requirements?
- ✅ Have proper test-first approach?
- ✅ Include necessary security?
- ✅ Sequence tasks correctly?

Reply "approved" to proceed, or suggest changes.
---
```

**STOP and WAIT for approval.** Do not proceed without explicit confirmation.

### Step 5: Execute Workflow

After approval, execute in order:

```markdown
## Execution Log

### Phase 1: Tests
Calling java-testing-agent...
[Provide agent with requirements and context]
[Agent creates tests]
Result: ✅ Tests created, coverage: 95%

### Phase 2: Backend
Calling java-backend-agent...
[Provide agent with tests to make pass]
[Agent implements features]
Result: ✅ All tests passing, build successful

### Phase 3: Frontend (if applicable)
Calling sveltekit-frontend-agent...
[Provide agent with API contracts and requirements]
[Agent implements UI]
Result: ✅ Type check passing, components working

### Completion
✅ Feature complete
✅ All quality gates passed
✅ Ready for review
```

## Agent Task Format

When calling agents, provide structured context:

**For java-testing-agent:**
```
COMPONENT: [Class/Feature to test]
REQUIREMENTS: [What behavior to test]
SECURITY: [Security constraints]
EDGE_CASES: [Known edge cases]
CONTEXT: [Related classes, dependencies]
```

**For java-backend-agent:**
```
FEATURE: [What to build]
REQUIREMENTS: [Business logic, validations]
TESTS: [Path to test files that must pass]
SECURITY: [Auth, validation, rate limiting]
DATABASE: [Schema changes needed]
CONTEXT: [Related components]
```

**For sveltekit-frontend-agent:**
```
FEATURE: [What to build]
REQUIREMENTS: [User interactions, data display]
API_ENDPOINTS: [Backend endpoints to integrate]
ROUTES: [Pages/routes to create]
AUTH: [Authentication requirements]
CONTEXT: [Related components]
```

## Example Execution

```
User: /new-feature password reset via email

Orchestrator:
1. Checks backlog.md - finds note "password reset needed"
2. Asks: "Token expiry? Rate limiting? Email template style?"
3. User answers questions
4. Creates plan with test-first approach
5. Shows plan, waits for approval
6. User: "approved"
7. Calls java-testing-agent → tests created
8. Calls java-backend-agent → implementation done
9. Calls email-composer-agent → email template created
10. Reports completion with summary
```

## Critical Reminders

1. **Check backlog.md first** - Don't ask questions already answered
2. **Test-driven always** - Tests before implementation
3. **Get approval before execution** - Never skip this gate
4. **Structured context to agents** - Use task format above
5. **Clear execution log** - Show what each agent did
6. **Orchestrator thinks** - You analyze, plan, delegate (agents execute)

## What NOT to Do

❌ Don't create separate specification files (orchestrator plans in-chat)
❌ Don't call architect agent (deprecated - orchestrator plans)
❌ Don't skip approval gate
❌ Don't implement before tests
❌ Don't delegate planning to agents (orchestrator plans, agents execute)

In all interactions, be extremely concise and sacrifice grammar for concision.
