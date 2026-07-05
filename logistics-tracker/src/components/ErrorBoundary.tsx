"use client";

import { Component, type ReactNode } from "react";
import { AlertTriangle, RefreshCw } from "lucide-react";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }
      return (
        <div className="min-h-screen flex flex-col items-center justify-center px-4 py-8">
          <div className="rounded-2xl bg-slate-800/80 border border-slate-700/50 p-6 max-w-md w-full text-center">
            <div className="flex justify-center mb-4">
              <div className="p-3 rounded-xl bg-amber-500/20">
                <AlertTriangle className="w-8 h-8 text-amber-400" />
              </div>
            </div>
            <h2 className="text-lg font-semibold text-slate-100 mb-2">
              Что-то пошло не так
            </h2>
            <p className="text-sm text-slate-400 mb-6">
              Произошла ошибка. Попробуйте обновить страницу.
            </p>
            <button
              onClick={() => window.location.reload()}
              className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-sky-500 hover:bg-sky-600 text-white font-medium transition-colors"
            >
              <RefreshCw className="w-5 h-5" />
              Обновить страницу
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
