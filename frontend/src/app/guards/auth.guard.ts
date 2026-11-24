import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route?: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.estaAutenticado()) {
    return router.createUrlTree(['/login']);
  }

  const rolesPermitidos = route?.data?.['roles'] as string[] | undefined;
  if (rolesPermitidos && rolesPermitidos.length > 0 && !authService.tieneRol(...rolesPermitidos)) {
    const rolActual = (authService.usuarioActual()?.rol || '').toUpperCase();
    if (rolActual === 'COCINA') {
      return router.createUrlTree(['/cocina']);
    }
    if (rolActual === 'ADMINISTRADOR') {
      return router.createUrlTree(['/crud']);
    }
    return router.createUrlTree(['/mesas']);
  }

  return true;
};
