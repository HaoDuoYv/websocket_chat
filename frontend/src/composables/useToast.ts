import { ref } from 'vue'

interface ToastOptions {
  message: string
  type?: 'success' | 'error' | 'warning' | 'info'
  duration?: number
}

const toastRef = ref<{ show: () => void } | null>(null)
const currentToast = ref<ToastOptions>({ message: '', type: 'info' })

export const useToast = () => {
  const setRef = (ref: any) => {
    toastRef.value = ref
  }

  const show = (options: ToastOptions | string) => {
    if (typeof options === 'string') {
      currentToast.value = { message: options, type: 'info' }
    } else {
      currentToast.value = options
    }
    // 延迟一帧确保 props 更新
    setTimeout(() => {
      toastRef.value?.show()
    }, 10)
  }

  const success = (message: string) => show({ message, type: 'success' })
  const error = (message: string) => show({ message, type: 'error' })
  const warning = (message: string) => show({ message, type: 'warning' })
  const info = (message: string) => show({ message, type: 'info' })

  return {
    setRef,
    show,
    success,
    error,
    warning,
    info,
    currentToast
  }
}
