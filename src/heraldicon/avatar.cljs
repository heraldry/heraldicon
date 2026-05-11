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

(defn shape-style [uncropped?]
  (if uncropped?
    {}
    {:border-radius "50%"}))
