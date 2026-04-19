# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security issues seriously. If you discover a security vulnerability in Moonstone, please report it responsibly.

### How to Report

1. **Do not** open a public GitHub issue for security vulnerabilities
2. Email the maintainers directly at the email associated with the repository
3. Include the following information:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Any suggested fixes (optional)

### What to Expect

- **Acknowledgment**: We will acknowledge receipt within 48 hours
- **Initial Assessment**: We will provide an initial assessment within 7 days
- **Resolution Timeline**: We aim to resolve critical issues within 30 days
- **Disclosure**: We will coordinate with you on public disclosure timing

### Scope

This security policy applies to:
- The Moonstone framework (core, desktop, android modules)
- The KleinLisp interpreter as used by Moonstone
- Official sample applications

### Out of Scope

- Third-party dependencies (please report to their maintainers)
- Applications built with Moonstone (please contact their developers)

## Security Best Practices

When building applications with Moonstone:

- Keep dependencies updated
- Validate user input in your Scheme code
- Use parameterized database queries (built into the ORM)
- Review third-party Scheme code before executing

## Acknowledgments

We appreciate responsible disclosure and will acknowledge security researchers who report valid vulnerabilities (with their permission).
