import React from 'react';
import {
  Dialog as MUIDialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  DialogContentText,
  Button,
  IconButton,
  DialogProps as MUIDialogProps
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

/**
 * Dialog - Material-UI Dialog Component Wrapper
 *
 * A modal dialog for displaying important information or collecting user input.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface DialogAction {
  /** Button label */
  label: string;
  /** Click handler */
  onClick: () => void;
  /** Button color */
  color?: 'inherit' | 'primary' | 'secondary' | 'success' | 'error' | 'info' | 'warning';
  /** Button variant */
  variant?: 'text' | 'outlined' | 'contained';
  /** Whether button is disabled */
  disabled?: boolean;
}

export enum DialogMaxWidth {
  XS = 'xs',
  SM = 'sm',
  MD = 'md',
  LG = 'lg',
  XL = 'xl'
}

export interface DialogProps {
  /** Whether the dialog is open */
  open: boolean;
  /** Close handler */
  onClose: () => void;
  /** Dialog title */
  title?: string;
  /** Dialog content */
  children: React.ReactNode;
  /** Content description (for accessibility) */
  description?: string;
  /** Primary action button */
  primaryAction?: DialogAction;
  /** Secondary action button */
  secondaryAction?: DialogAction;
  /** Additional actions */
  actions?: DialogAction[];
  /** Maximum width of the dialog */
  maxWidth?: DialogMaxWidth;
  /** Whether to take full width */
  fullWidth?: boolean;
  /** Whether to show close button in title */
  showCloseButton?: boolean;
  /** Whether clicking outside closes the dialog */
  disableBackdropClick?: boolean;
  /** Whether pressing Escape closes the dialog */
  disableEscapeKeyDown?: boolean;
  /** Whether the dialog is fullscreen */
  fullScreen?: boolean;
  /** Additional CSS class name */
  className?: string;
  /** Custom styles */
  sx?: MUIDialogProps['sx'];
}

/**
 * Dialog component for modal interactions
 *
 * @example
 * ```tsx
 * // Basic dialog
 * const [open, setOpen] = useState(false);
 * <Dialog
 *   open={open}
 *   onClose={() => setOpen(false)}
 *   title="Confirm Action"
 *   primaryAction={{ label: 'Confirm', onClick: handleConfirm }}
 *   secondaryAction={{ label: 'Cancel', onClick: () => setOpen(false) }}
 * >
 *   Are you sure you want to proceed?
 * </Dialog>
 *
 * // Form dialog
 * <Dialog
 *   open={open}
 *   onClose={() => setOpen(false)}
 *   title="Add User"
 *   maxWidth="sm"
 *   fullWidth
 * >
 *   <TextField label="Name" fullWidth />
 *   <TextField label="Email" fullWidth />
 * </Dialog>
 *
 * // Fullscreen dialog
 * <Dialog
 *   open={open}
 *   onClose={() => setOpen(false)}
 *   title="Full Details"
 *   fullScreen
 *   showCloseButton
 * >
 *   <DetailedContent />
 * </Dialog>
 * ```
 */
export const Dialog: React.FC<DialogProps> = ({
  open,
  onClose,
  title,
  children,
  description,
  primaryAction,
  secondaryAction,
  actions = [],
  maxWidth = DialogMaxWidth.SM,
  fullWidth = false,
  showCloseButton = false,
  disableBackdropClick = false,
  disableEscapeKeyDown = false,
  fullScreen = false,
  className,
  sx
}) => {
  const handleClose = (event: object, reason: 'backdropClick' | 'escapeKeyDown') => {
    if (reason === 'backdropClick' && disableBackdropClick) return;
    if (reason === 'escapeKeyDown' && disableEscapeKeyDown) return;
    onClose();
  };

  const allActions = [
    ...(secondaryAction ? [secondaryAction] : []),
    ...(primaryAction ? [primaryAction] : []),
    ...actions
  ];

  return (
    <MUIDialog
      open={open}
      onClose={handleClose}
      maxWidth={maxWidth as MUIDialogProps['maxWidth']}
      fullWidth={fullWidth}
      fullScreen={fullScreen}
      className={className}
      sx={sx}
    >
      {title && (
        <DialogTitle>
          {title}
          {showCloseButton && (
            <IconButton
              aria-label="close"
              onClick={onClose}
              sx={{
                position: 'absolute',
                right: 8,
                top: 8,
                color: (theme) => theme.palette.grey[500]
              }}
            >
              <CloseIcon />
            </IconButton>
          )}
        </DialogTitle>
      )}
      <DialogContent>
        {description && <DialogContentText>{description}</DialogContentText>}
        {children}
      </DialogContent>
      {allActions.length > 0 && (
        <DialogActions>
          {allActions.map((action, index) => (
            <Button
              key={index}
              onClick={action.onClick}
              color={action.color || 'primary'}
              variant={action.variant || 'text'}
              disabled={action.disabled}
            >
              {action.label}
            </Button>
          ))}
        </DialogActions>
      )}
    </MUIDialog>
  );
};

export default Dialog;
