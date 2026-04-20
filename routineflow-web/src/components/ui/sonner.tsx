import { Toaster as Sonner } from 'sonner'

type ToasterProps = React.ComponentProps<typeof Sonner>

const Toaster = ({ ...props }: ToasterProps) => (
  <Sonner
    theme="dark"
    className="toaster group"
    toastOptions={{
      classNames: {
        toast: 'group toast group-[.toaster]:bg-[#141414] group-[.toaster]:text-[#f5f5f7] group-[.toaster]:border-[#1f1f1f]',
        description: 'group-[.toast]:text-[#86868b]',
        actionButton: 'group-[.toast]:bg-[#0071e3] group-[.toast]:text-white',
        cancelButton: 'group-[.toast]:bg-[#1f1f1f] group-[.toast]:text-[#86868b]',
      },
    }}
    {...props}
  />
)

export { Toaster }
