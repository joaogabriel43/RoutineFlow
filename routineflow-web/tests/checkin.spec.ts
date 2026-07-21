import { test, expect } from '@playwright/test';

test.describe('RoutineFlow Golden Path', () => {
  test.beforeEach(async ({ page }) => {
    // Inject a fake token to bypass the RequireAuth guard
    await page.addInitScript(() => {
      window.localStorage.setItem('rf_token', 'fake-jwt-token-for-e2e');
    });
    
    // Mock user profile
    await page.route('**/api/auth/profile', async route => {
      await route.fulfill({ status: 200, json: { id: 1, name: 'Test User', email: 'test@example.com' } });
    });

    // Mock user preferences
    await page.route('**/api/preferences', async route => {
      await route.fulfill({
        status: 200,
        json: { theme: 'SYSTEM', firstDayOfWeek: 'MONDAY', soundEnabled: false }
      });
    });

    // Mock single tasks (empty for this test)
    await page.route('**/api/single-tasks/today**', async route => {
      await route.fulfill({ status: 200, json: [] });
    });
  });

  test('Deve ser possível completar uma tarefa e disparar o checkin', async ({ page }) => {
    // Mock the daily routine schedule
    await page.route('**/api/routines/active/day/**', async route => {
      await route.fulfill({
        status: 200,
        json: {
          dayOfWeek: 'MONDAY',
          areas: [
            {
              id: 1,
              name: 'Saúde',
              color: '#34D399',
              icon: 'heart',
              tasks: [
                {
                  id: 101,
                  title: 'Beber 2L de Água',
                  description: 'Manter-se hidratado',
                  estimatedMinutes: null,
                  goalType: 'BOOLEAN'
                }
              ]
            }
          ]
        }
      });
    });

    // Mock progress for the day
    await page.route('**/api/checkins/progress**', async route => {
      await route.fulfill({
        status: 200,
        json: {
          areas: [
            {
              areaId: 1,
              completedTasks: 0,
              totalTasks: 1,
              completionRate: 0,
              completedTaskIds: [],
              taskNotes: {},
              taskGoalProgress: {}
            }
          ]
        }
      });
    });

    // Intercept the check-in complete POST request
    let checkinPayload: any = null;
    let taskIdCalled: string | null = null;
    await page.route('**/api/checkins/*/complete**', async route => {
      if (route.request().method() === 'POST') {
        const url = route.request().url();
        const match = url.match(/\/checkins\/(\d+)\/complete/);
        if (match) taskIdCalled = match[1];
        
        checkinPayload = route.request().postDataJSON() || {};
        await route.fulfill({ status: 200, json: {} });
      } else {
        await route.continue();
      }
    });

    // Load the homepage
    await page.goto('/');

    // Check if the area and task are visible
    await expect(page.getByText('Saúde')).toBeVisible();
    await expect(page.getByText('Beber 2L de Água')).toBeVisible();

    // Click the checkbox (now highly accessible via our aria-label from Sprint 35)
    const taskCheckbox = page.getByRole('button', { name: /Marcar como concluída: Beber 2L de Água/i });
    await expect(taskCheckbox).toBeVisible();
    
    await taskCheckbox.click();

    // Wait for the API call to be intercepted
    // Note: Our application optimistic updates, so we just wait a tiny bit for the request
    await page.waitForTimeout(300);

    // Verify that the correct payload was sent to the backend
    expect(taskIdCalled).toBe('101');
  });
});
