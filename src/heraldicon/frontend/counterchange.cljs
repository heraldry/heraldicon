(ns heraldicon.frontend.counterchange
  (:require
   [heraldicon.heraldry.counterchange :as counterchange]
   [re-frame.core :as rf]))

(rf/reg-sub ::tinctures
  (fn [[_ {:keys [path]}] _]
    (rf/subscribe [:get path]))

  (fn [data [_ context]]
    (counterchange/get-tinctures data context)))

(defn tinctures [context]
  @(rf/subscribe [::tinctures context]))
