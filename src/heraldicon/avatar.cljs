(ns heraldicon.avatar
  (:require
   [heraldicon.config :as config]))

(defn url [username]
  (str (or (config/get :heraldicon-site-url)
           (config/get :heraldicon-url))
       "/avatar/" username))
