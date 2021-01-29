(ns heraldry.frontend.config
  (:refer-clojure :exclude [get])
  (:require-macros [heraldry.config])
  (:require [heraldry.config :as config]))

(defn get [setting]
  (case setting
    :armory-api-endpoint (heraldry.config/get-static :armory-api-endpoint)
    (config/get setting)))
