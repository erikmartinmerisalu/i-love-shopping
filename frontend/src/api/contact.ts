export type ContactPayload = {
  name: string;
  email: string;
  subject: string;
  message: string;
};

export class ContactApiError extends Error {
  fieldErrors: Record<string, string>;

  constructor(message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'ContactApiError';
    this.fieldErrors = fieldErrors;
  }
}

export async function submitContactForm(payload: ContactPayload): Promise<{ success: boolean; message: string }> {
  let response: Response;
  try {
    response = await fetch('/api/contact', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
  } catch {
    throw new ContactApiError('Network error. Please try again.');
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new ContactApiError(
      typeof data?.message === 'string' ? data.message : 'Could not send message',
      data?.fieldErrors ?? {}
    );
  }

  return data as { success: boolean; message: string };
}
