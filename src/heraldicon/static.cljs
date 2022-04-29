(ns heraldicon.static
  (:require
   [heraldicon.config :as config]))

(defn static-url [path]
  (str (config/get :static-files-url) path))
