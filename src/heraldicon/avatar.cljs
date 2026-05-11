(ns heraldicon.avatar
  (:require
   [heraldicon.config :as config]))

(def default-key "avatar/default.png")

(defn full-url
  "Constructs a public URL for an avatar S3 key (e.g. \"avatar/Xy7aB2.png\").
   Prepends :avatar-base-url if set, otherwise returns a path that resolves
   against the current host (which is wired through CloudFront to S3 in prod)."
  [key]
  (str (config/get :avatar-base-url) "/" key))

(defn url-from-user
  "Returns a renderable avatar URL for a user record. Falls back to the
   default avatar so the <img> still renders before user data has loaded
   or if the backend response omits :avatar-url."
  [user]
  (or (:avatar-url user)
      (full-url default-key)))

(defn shape-style [uncropped?]
  (if uncropped?
    {}
    {:border-radius "50%"}))
