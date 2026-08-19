import { useState, type FormEvent } from 'react';
import { ContactApiError, submitContactForm } from '../api/contact';
import PageMeta from '../components/PageMeta';
import { SITE } from '../config/site';

const fieldClass = (hasError: boolean) =>
  `w-full rounded-lg border bg-gray-950 px-3 py-2 text-sm outline-none focus:ring-2 ${
    hasError
      ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
      : 'border-gray-700 focus:border-primary focus:ring-primary/20'
  }`;

export default function ContactPage() {
  const [form, setForm] = useState({ name: '', email: '', subject: '', message: '' });
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);
    setSuccess(null);
    setFieldErrors({});
    setSubmitting(true);

    try {
      const result = await submitContactForm({
        name: form.name.trim(),
        email: form.email.trim(),
        subject: form.subject.trim(),
        message: form.message.trim(),
      });
      setSuccess(result.message);
      setForm({ name: '', email: '', subject: '', message: '' });
    } catch (error) {
      if (error instanceof ContactApiError) {
        setFormError(error.message);
        setFieldErrors(error.fieldErrors);
      } else {
        setFormError('Could not send your message. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageMeta
        title="Contact & Support"
        description="Demo contact form — placeholder details only, no real support desk."
      />

      <div className="page-container py-12">
        {SITE.isDemoSite && (
          <p className="mb-8 max-w-3xl rounded-lg border border-amber-600/40 bg-amber-950/30 px-4 py-3 text-sm text-amber-100">
            This is a sandbox contact form for the demo shop. Submissions are validated and may be logged or emailed
            to a developer inbox if mail is configured — there is no real customer support team or office at the
            address below.
          </p>
        )}

        <div className="grid gap-10 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.3fr)]">
          <section>
            <h1 className="text-3xl font-bold">Contact & support (demo)</h1>
            <p className="mt-3 text-gray-300 leading-relaxed">
              Use the form to test validation and API integration. Do not enter sensitive personal data — this is a
              student project environment.
            </p>

            <div className="mt-8 rounded-xl border border-dashed border-gray-700 bg-gray-900/50 p-5">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-400">Placeholder details</h2>
              <dl className="mt-4 space-y-3 text-sm">
                <div>
                  <dt className="text-gray-500">Demo inbox</dt>
                  <dd className="font-mono text-gray-300">{SITE.contact.email}</dd>
                </div>
                <div>
                  <dt className="text-gray-500">Demo phone</dt>
                  <dd className="text-gray-300">{SITE.contact.phone}</dd>
                </div>
                <div>
                  <dt className="text-gray-500">Fictional address</dt>
                  <dd className="text-gray-300">{SITE.contact.address}</dd>
                </div>
                <div>
                  <dt className="text-gray-500">Hours</dt>
                  <dd className="text-gray-300">{SITE.contact.hours}</dd>
                </div>
              </dl>
            </div>

            <p className="mt-6 text-xs text-gray-500">
              Social links on this project are example.com placeholders and are not linked from this page.
            </p>
          </section>

          <section className="surface-panel p-6">
            <h2 className="text-lg font-semibold">Send a test message</h2>
            <p className="mt-1 text-xs text-gray-500">Try subject lines like “Order question” or “Delivery demo”.</p>
            <form onSubmit={(event) => void handleSubmit(event)} className="mt-6 space-y-4" noValidate>
              {formError && (
                <div
                  className="rounded-lg border border-red-700 bg-red-900/40 px-3 py-2 text-sm text-red-200"
                  role="alert"
                >
                  {formError}
                </div>
              )}
              {success && (
                <div
                  className="rounded-lg border border-green-700 bg-green-900/30 px-3 py-2 text-sm text-green-200"
                  role="status"
                >
                  {success}
                </div>
              )}

              <label className="block space-y-1 text-sm">
                <span>Your name</span>
                <input
                  className={fieldClass(!!fieldErrors.name)}
                  value={form.name}
                  onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                  autoComplete="name"
                  placeholder="Demo User"
                  required
                />
                {fieldErrors.name && <span className="text-xs text-red-400">{fieldErrors.name}</span>}
              </label>

              <label className="block space-y-1 text-sm">
                <span>Email</span>
                <input
                  type="email"
                  className={fieldClass(!!fieldErrors.email)}
                  value={form.email}
                  onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                  autoComplete="email"
                  placeholder="you@example.com"
                  required
                />
                {fieldErrors.email && <span className="text-xs text-red-400">{fieldErrors.email}</span>}
              </label>

              <label className="block space-y-1 text-sm">
                <span>Subject</span>
                <input
                  className={fieldClass(!!fieldErrors.subject)}
                  value={form.subject}
                  onChange={(event) => setForm((current) => ({ ...current, subject: event.target.value }))}
                  placeholder="Demo inquiry"
                  required
                />
                {fieldErrors.subject && <span className="text-xs text-red-400">{fieldErrors.subject}</span>}
              </label>

              <label className="block space-y-1 text-sm">
                <span>Message</span>
                <textarea
                  className={`${fieldClass(!!fieldErrors.message)} min-h-[8rem] resize-y`}
                  value={form.message}
                  onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))}
                  placeholder="This is a test message for the coursework demo…"
                  required
                />
                {fieldErrors.message && <span className="text-xs text-red-400">{fieldErrors.message}</span>}
              </label>

              <button
                type="submit"
                disabled={submitting}
                className="w-full rounded-lg bg-primary py-3 font-semibold text-white hover:bg-primary-focus disabled:opacity-50"
              >
                {submitting ? 'Sending…' : 'Send demo message'}
              </button>
            </form>
          </section>
        </div>
      </div>
    </>
  );
}
