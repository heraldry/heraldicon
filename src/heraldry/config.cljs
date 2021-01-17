(ns heraldry.config
  (:refer-clojure :exclude [get])
  (:require-macros [heraldry.config]))

(defn -js->clj+
  "For cases when built-in js->clj doesn't work. Source: https://stackoverflow.com/a/32583549/4839573"
  [x]
  (into {} (for [k (js-keys x)]
             [(keyword k) (aget x k)])))

(def env
  "Returns current env vars as a Clojure map."
  (-js->clj+ (.-env js/process)))

(def region
  (or (:REGION env) (heraldry.config/get-static :region)))

(def stage
  (or (:STAGE env) (heraldry.config/get-static :stage)))

(defn get [setting]
  (case setting
    :region region
    :stage  stage
    nil))
