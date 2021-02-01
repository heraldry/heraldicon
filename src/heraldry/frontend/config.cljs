(ns heraldry.frontend.config
  (:refer-clojure :exclude [get])
  (:require-macros [heraldry.config])
  (:require [heraldry.config :as config]))

(defn get [setting]
  (case setting
    :heraldry-api-endpoint (heraldry.config/get-static :heraldry-api-endpoint)
    (config/get setting)))
