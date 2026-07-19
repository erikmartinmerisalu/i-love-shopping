import { Link } from "react-router-dom";

const RegisterPage = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900 text-white">
      <div className="w-full max-w-md p-6 bg-gray-800 rounded-xl shadow-lg">

        <h2 className="text-2xl font-semibold text-center mb-4">
          Register
        </h2>

        <form className="space-y-3">
          <input
            type="text"
            placeholder="DisplayName"
            className="w-full p-2 rounded bg-gray-700 border border-gray-600"
          />

          <input
            type="email"
            placeholder="Email"
            className="w-full p-2 rounded bg-gray-700 border border-gray-600"
          />

          <input
            type="password"
            placeholder="Password"
            className="w-full p-2 rounded bg-gray-700 border border-gray-600"
          />

          <input
            type="password"
            placeholder="Confirm password"
            className="w-full p-2 rounded bg-gray-700 border border-gray-600"
          />

          <button className="w-full bg-white text-black py-2 rounded">
            Create account
          </button>
        </form>

        <p className="text-center text-sm mt-4 text-gray-400">
          Already have an account?{" "}
          <Link to="/login" className="text-white underline">
            Login
          </Link>
        </p>
      </div>
    </div>
  );
};

export default RegisterPage;
