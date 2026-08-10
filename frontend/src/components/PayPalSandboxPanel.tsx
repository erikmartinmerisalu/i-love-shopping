import { useState } from 'react';
import { confirmSandboxPayment, PaymentApiError, type PaymentResultResponse } from '../api/payments';

type Props = {
  orderNumber: string;
  amount: number;
  onPaid: (result: PaymentResultResponse) => void;
  onPaymentFailed?: (result: PaymentResultResponse) => void;
};

const SCENARIOS = [
  {
    scenario: 'success',
    label: 'Approve with PayPal',
    description: 'Simulates returning from PayPal after the buyer approves payment.',
    className: 'bg-[#0070ba] hover:bg-[#005ea6] text-white',
  },
  {
    scenario: 'insufficient_funds',
    label: 'Insufficient PayPal balance',
    description: 'Simulates PayPal declining due to insufficient balance.',
    className: 'border border-amber-500/50 bg-amber-900/30 text-amber-100 hover:bg-amber-900/50',
  },
  {
    scenario: 'timeout',
    label: 'PayPal gateway timeout',
    description: 'Simulates a timeout while PayPal processes the payment.',
    className: 'border border-gray-600 bg-gray-900 text-gray-200 hover:bg-gray-800',
  },
] as const;

const PayPalSandboxPanel = ({ orderNumber, amount, onPaid, onPaymentFailed }: Props) => {
  const [submitting, setSubmitting] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const runScenario = async (scenario: string) => {
    setSubmitting(scenario);
    setError(null);
    try {
      const result = await confirmSandboxPayment(orderNumber, scenario);
      if (result.success) {
        onPaid(result);
      } else {
        setError(result.message || 'PayPal payment could not be completed');
        onPaymentFailed?.(result);
      }
    } catch (err) {
      setError(err instanceof PaymentApiError ? err.message : 'PayPal payment failed');
    } finally {
      setSubmitting(null);
    }
  };

  return (
    <div className="space-y-4">
      <div className="rounded-xl border border-[#0070ba]/40 bg-[#0070ba]/10 p-4 text-sm text-sky-50">
        <p className="font-semibold text-[#9fd4ff]">PayPal sandbox simulation</p>
        <p className="mt-1 text-xs text-sky-100/80">
          No card details are used for PayPal. Choose an outcome below to simulate the PayPal
          redirect and approval flow. Amount: €{Number(amount).toFixed(2)}
        </p>
      </div>

      {error && (
        <div className="rounded-lg border border-red-700 bg-red-900/40 px-3 py-2 text-sm text-red-200">
          {error}
        </div>
      )}

      <div className="space-y-3">
        {SCENARIOS.map((option) => (
          <button
            key={option.scenario}
            type="button"
            disabled={submitting !== null}
            onClick={() => void runScenario(option.scenario)}
            className={`w-full rounded-lg px-4 py-3 text-left text-sm font-semibold transition disabled:opacity-50 ${option.className}`}
          >
            <span>{submitting === option.scenario ? 'Processing…' : option.label}</span>
            <span className="mt-1 block text-xs font-normal opacity-90">{option.description}</span>
          </button>
        ))}
      </div>
    </div>
  );
};

export default PayPalSandboxPanel;
