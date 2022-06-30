(ns heraldicon.frontend.counterchange
  (:require
   [heraldicon.heraldry.counterchange :as counterchange]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(rf/reg-sub ::tinctures
  (fn [[_ path _context] _]
    (rf/subscribe [:get path]))

  (fn [data [_ _path context]]
    (counterchange/get-tinctures data context)))

(defn tinctures [{:keys [path] :as context}]
  (if (-> path first (= :context))
    (counterchange/get-tinctures (interface/get-raw-data context) context)
    @(rf/subscribe [::tinctures path context])))
