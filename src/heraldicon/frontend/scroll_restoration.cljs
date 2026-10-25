(ns heraldicon.frontend.scroll-restoration
  (:require
   [heraldicon.config :as config]))

;; The browser's own restoration runs before a client-rendered page has its content,
;; so positions are kept per URL and restored once the page is tall enough.

(defonce ^:private positions
  (atom {}))

(defonce ^:private history-navigation?
  (atom false))

(defonce ^:private pending-restore
  (atom nil))

(defonce ^:private last-url
  (atom nil))

(def ^:private max-restore-frames
  120)

(defn- current-url []
  (str js/window.location.pathname js/window.location.search))

(defn- save-position []
  ;; scroll events while a page is still rendering would overwrite the position to restore
  (when-not @pending-restore
    (swap! positions assoc (current-url) js/window.scrollY)))

(defn- restore [token y frames-left]
  (when (identical? @pending-restore token)
    (let [max-y (- js/document.documentElement.scrollHeight js/window.innerHeight)]
      (if (or (>= max-y y)
              (zero? frames-left))
        (do
          (js/window.scrollTo 0 y)
          (reset! pending-restore nil))
        (js/requestAnimationFrame #(restore token y (dec frames-left)))))))

(defn after-navigation []
  (when-not (config/get :backend?)
    (let [url (current-url)
          same-url? (= url @last-url)]
      (reset! last-url url)
      (cond
        @history-navigation? (let [token #js {}
                                   y (get @positions url 0)]
                               (reset! history-navigation? false)
                               (reset! pending-restore token)
                               ;; the first frame still shows the previous page
                               (js/requestAnimationFrame
                                #(js/requestAnimationFrame
                                  (fn [] (restore token y max-restore-frames)))))
        ;; e.g. a hot reload or a replace-state
        same-url? nil
        :else (do
                (reset! pending-restore nil)
                (js/window.scrollTo 0 0))))))

(defonce ^:private started?
  (atom false))

;; must run before the router starts, so its popstate listener comes first
(defn start []
  (when-not @started?
    (reset! started? true)
    (set! js/window.history.scrollRestoration "manual")
    (.addEventListener js/window "popstate" #(reset! history-navigation? true))
    (.addEventListener js/window "scroll" save-position #js {:passive true})))
