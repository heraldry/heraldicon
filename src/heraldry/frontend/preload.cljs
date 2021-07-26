(ns heraldry.frontend.preload
  (:require [devtools.core :as devtools]))

(defn setup-devtools []
  (let [{:keys [cljs-land-style]} (devtools/get-prefs)]
    (devtools/set-pref! :cljs-land-style (str "filter:invert(1);" cljs-land-style)))
  (devtools/install!))

(setup-devtools)
