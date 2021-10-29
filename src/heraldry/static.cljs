(ns heraldry.static
  (:require
   [heraldry.config :as config]))

(defn static-url [path]
  (str (config/get :static-files-url) path))
