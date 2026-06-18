import { Toaster as Sonner } from 'sonner'

type ToasterProps = React.ComponentProps<typeof Sonner>

const Toaster = ({ ...props }: ToasterProps) => (
  <Sonner
    theme="dark"
    className="toaster group"
    toastOptions={{
      classNames: {
        toast: 'group toast group-[.toaster]:bg-[#141416] group-[.toaster]:text-[#F4F2EF] group-[.toaster]:border-[#26262A]',
        description: 'group-[.toast]:text-[#8C8A88]',
        actionButton: 'group-[.toast]:bg-[#2F8BFF] group-[.toast]:text-white',
        cancelButton: 'group-[.toast]:bg-[#26262A] group-[.toast]:text-[#8C8A88]',
      },
    }}
    {...props}
  />
)

export { Toaster }
