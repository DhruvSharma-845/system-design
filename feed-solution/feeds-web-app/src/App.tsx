import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type AppRoute = '/login' | '/post/new'

type DecodedJwt = {
  exp?: number
  realm_access?: {
    roles?: string[]
  }
}

const TOKEN_STORAGE_KEY = 'feed_access_token'
const PKCE_VERIFIER_KEY = 'feed_pkce_verifier'
const OIDC_STATE_KEY = 'feed_oidc_state'
const REQUIRED_ROLES = new Set(['feed_user', 'feed_moderator', 'feed_admin'])
const CLIENT_ID = 'feed-frontend'

const getRouteFromPath = (path: string): AppRoute => (path === '/post/new' ? '/post/new' : '/login')

const navigateTo = (route: AppRoute) => {
  if (window.location.pathname !== route) {
    window.history.pushState({}, '', route)
  }
}

const decodeJwtPayload = (token: string): DecodedJwt | null => {
  try {
    const payloadPart = token.split('.')[1]
    if (!payloadPart) {
      return null
    }

    const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4)
    const payload = atob(padded)
    return JSON.parse(payload) as DecodedJwt
  } catch {
    return null
  }
}

const isAuthorizedToken = (token: string): boolean => {
  const decoded = decodeJwtPayload(token)
  const roles = decoded?.realm_access?.roles ?? []
  return roles.some((role) => REQUIRED_ROLES.has(role))
}

const isTokenExpired = (token: string): boolean => {
  const decoded = decodeJwtPayload(token)
  if (!decoded?.exp) {
    return true
  }

  const nowSeconds = Math.floor(Date.now() / 1000)
  return decoded.exp <= nowSeconds
}

const randomBase64Url = (bytesLength: number): string => {
  const bytes = new Uint8Array(bytesLength)
  crypto.getRandomValues(bytes)
  let binary = ''
  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

const sha256FallbackBase64Url = (value: string): string => {
  const rightRotate = (n: number, bits: number) => (n >>> bits) | (n << (32 - bits))
  const bytes = new TextEncoder().encode(value)
  const bitLength = bytes.length * 8
  const withOne = new Uint8Array(bytes.length + 1)
  withOne.set(bytes)
  withOne[bytes.length] = 0x80

  const remainder = withOne.length % 64
  const padLength = remainder <= 56 ? 56 - remainder : 56 + (64 - remainder)
  const message = new Uint8Array(withOne.length + padLength + 8)
  message.set(withOne)

  const view = new DataView(message.buffer)
  const high = Math.floor(bitLength / 2 ** 32)
  const low = bitLength >>> 0
  view.setUint32(message.length - 8, high)
  view.setUint32(message.length - 4, low)

  const h = [
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
  ]
  const k = [
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
  ]

  for (let offset = 0; offset < message.length; offset += 64) {
    const w = new Uint32Array(64)
    for (let i = 0; i < 16; i += 1) {
      w[i] = view.getUint32(offset + i * 4)
    }
    for (let i = 16; i < 64; i += 1) {
      const s0 = rightRotate(w[i - 15], 7) ^ rightRotate(w[i - 15], 18) ^ (w[i - 15] >>> 3)
      const s1 = rightRotate(w[i - 2], 17) ^ rightRotate(w[i - 2], 19) ^ (w[i - 2] >>> 10)
      w[i] = (((w[i - 16] + s0) >>> 0) + ((w[i - 7] + s1) >>> 0)) >>> 0
    }

    let [a, b, c, d, e, f, g, hh] = h
    for (let i = 0; i < 64; i += 1) {
      const s1 = rightRotate(e, 6) ^ rightRotate(e, 11) ^ rightRotate(e, 25)
      const ch = (e & f) ^ (~e & g)
      const temp1 = (((((hh + s1) >>> 0) + ch) >>> 0) + ((k[i] + w[i]) >>> 0)) >>> 0
      const s0 = rightRotate(a, 2) ^ rightRotate(a, 13) ^ rightRotate(a, 22)
      const maj = (a & b) ^ (a & c) ^ (b & c)
      const temp2 = (s0 + maj) >>> 0

      hh = g
      g = f
      f = e
      e = (d + temp1) >>> 0
      d = c
      c = b
      b = a
      a = (temp1 + temp2) >>> 0
    }

    h[0] = (h[0] + a) >>> 0
    h[1] = (h[1] + b) >>> 0
    h[2] = (h[2] + c) >>> 0
    h[3] = (h[3] + d) >>> 0
    h[4] = (h[4] + e) >>> 0
    h[5] = (h[5] + f) >>> 0
    h[6] = (h[6] + g) >>> 0
    h[7] = (h[7] + hh) >>> 0
  }

  const hash = new Uint8Array(32)
  const hashView = new DataView(hash.buffer)
  for (let i = 0; i < h.length; i += 1) {
    hashView.setUint32(i * 4, h[i])
  }

  let binary = ''
  for (const byte of hash) {
    binary += String.fromCharCode(byte)
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

const sha256Base64Url = async (value: string): Promise<string> => {
  const subtleCrypto = globalThis.crypto?.subtle
  if (!subtleCrypto) {
    return sha256FallbackBase64Url(value)
  }
  const data = new TextEncoder().encode(value)
  const digest = await subtleCrypto.digest('SHA-256', data)
  const bytes = new Uint8Array(digest)
  let binary = ''
  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function App() {
  const [route, setRoute] = useState<AppRoute>(getRouteFromPath(window.location.pathname))
  const [content, setContent] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [statusMessage, setStatusMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_STORAGE_KEY))

  useEffect(() => {
    const onPopState = () => setRoute(getRouteFromPath(window.location.pathname))
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  useEffect(() => {
    if (!token) {
      if (route !== '/login') {
        navigateTo('/login')
        setRoute('/login')
      }
      return
    }

    if (isTokenExpired(token) || !isAuthorizedToken(token)) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      setToken(null)
      setErrorMessage('You are authenticated but not authorized to create posts.')
      navigateTo('/login')
      setRoute('/login')
      return
    }

    if (route !== '/post/new') {
      navigateTo('/post/new')
      setRoute('/post/new')
    }
  }, [route, token])

  const keycloakBaseUrl = useMemo(() => `${window.location.origin}/realms/feed/protocol/openid-connect`, [])
  const redirectUri = useMemo(() => `${window.location.origin}/login`, [])

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    const state = params.get('state')
    const error = params.get('error')
    const errorDescription = params.get('error_description')

    if (error) {
      const message = errorDescription ? `${error}: ${errorDescription}` : error
      setErrorMessage(`Authentication failed: ${message}`)
      window.history.replaceState({}, '', '/login')
      setRoute('/login')
      return
    }

    if (!code) {
      return
    }

    const verifier = sessionStorage.getItem(PKCE_VERIFIER_KEY)
    const expectedState = sessionStorage.getItem(OIDC_STATE_KEY)
    if (!verifier || !state || state !== expectedState) {
      setErrorMessage('Invalid authentication state. Please try logging in again.')
      window.history.replaceState({}, '', '/login')
      setRoute('/login')
      return
    }

    const exchangeCode = async () => {
      let loginCompleted = false
      try {
        setIsLoading(true)
        const tokenParams = new URLSearchParams({
          client_id: CLIENT_ID,
          grant_type: 'authorization_code',
          code,
          redirect_uri: redirectUri,
          code_verifier: verifier,
        })

        const response = await fetch(`${keycloakBaseUrl}/token`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: tokenParams.toString(),
        })

        const responseText = await response.text()
        if (!response.ok) {
          throw new Error(`Token exchange failed (${response.status}): ${responseText || 'No response body.'}`)
        }

        const authData = JSON.parse(responseText) as { access_token?: string }
        if (!authData.access_token) {
          throw new Error('Token endpoint did not return an access token.')
        }

        if (isTokenExpired(authData.access_token) || !isAuthorizedToken(authData.access_token)) {
          throw new Error('Authenticated user is not authorized to create posts.')
        }

        localStorage.setItem(TOKEN_STORAGE_KEY, authData.access_token)
        setToken(authData.access_token)

        // Persist user in app DB after Keycloak login/signup (idempotent)
        try {
          await fetch('/api/v1/users/signup', {
            method: 'POST',
            headers: {
              Authorization: `Bearer ${authData.access_token}`,
            },
          })
        } catch {
          // Non-fatal: user can still use the app; signup sync may be retried on next login
        }

        setStatusMessage('Login successful.')
        setErrorMessage('')
        loginCompleted = true
        navigateTo('/post/new')
        setRoute('/post/new')
      } catch (exchangeError) {
        localStorage.removeItem(TOKEN_STORAGE_KEY)
        setToken(null)
        setErrorMessage(exchangeError instanceof Error ? exchangeError.message : 'Authentication failed.')
      } finally {
        sessionStorage.removeItem(PKCE_VERIFIER_KEY)
        sessionStorage.removeItem(OIDC_STATE_KEY)
        window.history.replaceState({}, '', loginCompleted ? '/post/new' : '/login')
        setIsLoading(false)
      }
    }

    void exchangeCode()
  }, [keycloakBaseUrl, redirectUri])

  const startOidcFlow = async (isSignup: boolean) => {
    setStatusMessage('')
    setErrorMessage('')

    try {
      setIsLoading(true)
      const verifier = randomBase64Url(64)
      const challenge = await sha256Base64Url(verifier)
      const state = randomBase64Url(32)

      sessionStorage.setItem(PKCE_VERIFIER_KEY, verifier)
      sessionStorage.setItem(OIDC_STATE_KEY, state)

      const params = new URLSearchParams({
        client_id: CLIENT_ID,
        response_type: 'code',
        scope: 'openid profile email',
        redirect_uri: redirectUri,
        code_challenge_method: 'S256',
        code_challenge: challenge,
        state,
      })

      if (isSignup) {
        params.set('kc_action', 'register')
      }

      window.location.assign(`${keycloakBaseUrl}/auth?${params.toString()}`)
    } catch (flowError) {
      setErrorMessage(flowError instanceof Error ? flowError.message : 'Unable to start authentication flow.')
      setIsLoading(false)
    } finally {
      // Browser navigation will normally occur before this line executes.
    }
  }

  const logout = () => {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    setToken(null)
    setStatusMessage('Logged out.')
    navigateTo('/login')
    setRoute('/login')
  }

  const submitPost = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setStatusMessage('')
    setErrorMessage('')

    const trimmedContent = content.trim()
    if (!trimmedContent) {
      setErrorMessage('Post content is required.')
      return
    }

    if (!token) {
      setErrorMessage('Session expired. Please log in again.')
      navigateTo('/login')
      setRoute('/login')
      return
    }

    try {
      setIsLoading(true)
      const response = await fetch('/api/v1/posts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ content: trimmedContent }),
      })

      const responseText = await response.text()
      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          logout()
          throw new Error('Session expired or unauthorized. Please log in again.')
        }

        throw new Error(`Post creation failed (${response.status}): ${responseText || 'No response body.'}`)
      }

      setStatusMessage('Post created successfully.')
      setContent('')
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Unexpected error while creating the post.')
    } finally {
      setIsLoading(false)
    }
  }

  if (!token || route === '/login') {
    return (
      <main className="app-shell">
        <h1>Feed Login</h1>
        <p className="subtle">
          You will be redirected to Keycloak for secure login/signup. After authentication, you return here
          with an authorization code and the app exchanges it for an access token.
        </p>
        <div className="action-row">
          <button type="button" disabled={isLoading} onClick={() => void startOidcFlow(false)}>
            {isLoading ? 'Redirecting...' : 'Login with Keycloak'}
          </button>
          <button type="button" className="secondary-action button-like-link" disabled={isLoading} onClick={() => void startOidcFlow(true)}>
            Sign Up
          </button>
        </div>
        {statusMessage ? <p className="status success">{statusMessage}</p> : null}
        {errorMessage ? <p className="status error">{errorMessage}</p> : null}
      </main>
    )
  }

  return (
    <main className="app-shell">
      <h1>Create New Post</h1>
      <form className="post-form" onSubmit={submitPost}>
        <label htmlFor="post-content">Content</label>
        <textarea
          id="post-content"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          rows={8}
          placeholder="Write your post..."
          disabled={isLoading}
        />
        <div className="action-row">
          <button type="submit" disabled={isLoading}>
            {isLoading ? 'Submitting...' : 'Submit'}
          </button>
          <button type="button" className="secondary-action button-like-link" onClick={logout}>
            Logout
          </button>
        </div>
      </form>

      {statusMessage ? <p className="status success">{statusMessage}</p> : null}
      {errorMessage ? <p className="status error">{errorMessage}</p> : null}
    </main>
  )
}

export default App
