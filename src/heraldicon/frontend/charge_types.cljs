(ns heraldicon.frontend.charge-types
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as str]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.context :as c]
   [heraldicon.entity.user :as entity.user]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.repository.charge-types :as repository.charge-types]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.user.form.core :as form]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def form-db-path
  (form/form-path ::form))

(def search-db-path
  (conj form-db-path ::search))

(history/register-undoable-path form-db-path)

(def base-context
  (c/<< context/default :path form-db-path))

(rf/reg-event-fx ::save
  (fn [{:keys [db]} _]
    (let [session (session/data-from-db db)
          data (get-in db form-db-path)]
      (go
        (try
          (let [result (<? (api/call :save-charge-types data session))]
            (rf/dispatch [:set form-db-path result])
            (rf/dispatch-sync [::message/set-success ::id "Updated"])
            (js/setTimeout #(rf/dispatch [::message/clear ::id]) 3000))

          (catch :default error
            (rf/dispatch [::message/set-error ::id (:message (ex-data error))])
            (log/error error "save charge-types error"))))

      {})))

(defn- search-bar
  []
  [:div {:style {:margin-bottom "10px"}}
   [:input {:type "text"
            :placeholder "Search"
            :style {:width "20em"}
            :on-change (fn [event]
                         (rf/dispatch [:set search-db-path (-> event .-target .-value)]))}]])

(defn- search
  [title]
  (let [title (str/lower-case (or title ""))
        words (str/split (-> @(rf/subscribe [:get search-db-path])
                             (or "")
                             str/lower-case)
                         #" +")]
    (if (empty? words)
      true
      (reduce (fn [_ word]
                (when (str/includes? title word)
                  (reduced true)))
              nil
              words))))

(defn- charge-type-editor
  []
  (rf/dispatch [::title/set :string.menu/charge-types])
  (status/default
   (rf/subscribe [::repository.charge-types/data #(rf/dispatch [:set form-db-path %])])
   (fn [_]
     [:div {:style {:position "relative"
                    :max-width "40em"
                    :height "calc(100vh - 4em)"
                    :padding "10px"}}
      [:div {:style {:height "calc(100% - 2.5em)"
                     :overflow "scroll"}}
       [search-bar]
       [history/buttons form-db-path]
       [tree/tree [form-db-path] base-context :search-fn search]]

      [:button.button.primary {:type "submit"
                               :on-click (fn [event]
                                           (.preventDefault event)
                                           (.stopPropagation event)
                                           (rf/dispatch [::save]))
                               :style {:float "right"
                                       :margin-bottom "10px"}}
       [tr :string.button/save]]
      [:div {:style {:width "80%"
                     :float "left"}}
       [message/display ::id]]])))

(defn view []
  (if (entity.user/admin? @(rf/subscribe [::session/data]))
    [charge-type-editor]
    [:div]))

(rf/reg-sub ::name-map
  :<- [:get form-db-path]

  (fn [data _]
    (let [types (tree-seq
                 (fn [[data _path]]
                   (:type data))
                 (fn [[data path]]
                   (map-indexed (fn [index item]
                                  [item (conj path index)])
                                (:types data)))
                 [data form-db-path])
          name-map (-> (group-by (comp :name first) types)
                       (update-vals (fn [value]
                                      (mapv (fn [[_item path]]
                                              path)
                                            value))))]
      name-map)))
