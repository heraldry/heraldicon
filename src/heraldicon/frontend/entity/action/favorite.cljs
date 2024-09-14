(ns heraldicon.frontend.entity.action.favorite
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def favorite-count-path
  [::store :counts])

(def user-favorites-path
  [::store :user-favorites])

(defn set-favorite-count
  [db entity-id favorite-count]
  (assoc-in db (conj favorite-count-path entity-id) favorite-count))

(defn set-is-user-favorite
  [db entity-id is-user-favorite]
  (assoc-in db (conj user-favorites-path entity-id) is-user-favorite))

(rf/reg-sub ::favorite-count
  (fn [[_ entity-id]]
    (rf/subscribe [:get (conj favorite-count-path entity-id)]))

  (fn [favorite-count _]
    (or favorite-count 0)))

(rf/reg-sub ::is-user-favorite?
  (fn [[_ entity-id]]
    (rf/subscribe [:get (conj user-favorites-path entity-id)]))

  (fn [is-user-favorite? _]
    is-user-favorite?))

(rf/reg-event-db ::update-favorite
  (fn [db [_ entity-id {:keys [is-user-favorite
                               favorite-count]}]]
    (-> db
        (set-favorite-count entity-id favorite-count)
        (set-is-user-favorite entity-id is-user-favorite))))

(rf/reg-fx ::toggle-favorite
  (fn [[entity-id session]]
    (go
      (try
        (let [;; TODO: this can probably live elsewhere
              payload {:entity-id entity-id}
              result (<? (api/call :toggle-favorite payload session))]
          (rf/dispatch [::update-favorite entity-id result]))

        (catch :default e
          (log/error e "favorite error"))))))

(rf/reg-event-fx ::invoke
  (fn [_ [_ entity-id session]]
    {::toggle-favorite [entity-id session]}))

(defn action [entity-id]
  (let [logged-in? @(rf/subscribe [::session/logged-in?])
        can-favorite? (and logged-in?
                           entity-id)
        session @(rf/subscribe [::session/data])]
    {:title :string.button/share
     :handler (when can-favorite?
                #(rf/dispatch [::invoke entity-id session]))
     :disabled? (not can-favorite?)
     :tooltip (when-not logged-in?
                :string.user.message/need-to-be-logged-in)}))

(defn icon [height on?]
  [:svg {:version "1.1"
         :xmlns "http://www.w3.org/2000/svg"
         :viewBox "-1 -1 27 25"
         :style {:height (str height "px")
                 :vertical-align "top"}}
   [:path {:d "M 17.736302,0
                  C 21.748672,0 25,3.2738716 25,7.3140594 25,9.6415126 23.444994,11.863869 22.298625,13.114203
                  L 12.499998,23 2.7013743,13.114203
                  C 1.5550053,11.863869 0,9.6415126 0,7.3140594 0,3.2738716 3.2513274,0 7.2636967,0 9.3201851,0 11.178305,0.85924472 12.499998,2.2420263 13.821695,0.85924472 15.679814,0 17.736302,0
                  Z"
           :style {:stroke-width 1
                   :stroke "#000"
                   :fill (if on?
                           "red"
                           "none")}}]])

(defn button [entity-id & {:keys [height]
                           :or {height 24}}]
  (let [{:keys [handler
                disabled?
                tooltip]} (action entity-id)
        favorite-count @(rf/subscribe [::favorite-count entity-id])
        is-favorite? @(rf/subscribe [::is-user-favorite? entity-id])]
    [:button.button.favorite
     {:style {:flex "initial"
              :padding "5px"
              :line-height (str height "px")}
      :class (when disabled? "disabled")
      :title (tr tooltip)
      :on-click handler}
     [icon height is-favorite?]
     [:div {:style {:display "inline-block"
                    :padding-left "5px"
                    :font-size (str (* height 0.8) "px")
                    :vertical-align "top"}}
      favorite-count]]))
