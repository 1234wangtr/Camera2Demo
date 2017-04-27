package com.example.camera2demo;

import java.nio.ByteBuffer;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

public class Camera2Demo extends Activity implements
		TextureView.SurfaceTextureListener {

	private CameraDevice mCamera;
	private String mCameraID = "1";
	
	private TextureView mPreviewView;
	private Size mPreviewSize;
	private CaptureRequest.Builder mPreviewBuilder;
	private ImageReader mImageReader;
	
	private Handler mHandler;
	private HandlerThread mThreadHandler;

	// ���ﶨ�����ImageReader�ص���ͼƬ�Ĵ�С
	private int mImageWidth = 1920;
	private int mImageHeight = 1080;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera);

		initView();
		initLooper();

	}

	// �ܶ���̶�������첽���ˣ�����������Ҫһ�����̵߳�looper
	private void initLooper() {
		mThreadHandler = new HandlerThread("CAMERA2");
		mThreadHandler.start();
		mHandler = new Handler(mThreadHandler.getLooper());
	}

	// ����ͨ��TextureView����SurfaceView
	private void initView() {
		mPreviewView = (TextureView) findViewById(R.id.textureview);
		mPreviewView.setSurfaceTextureListener(this);
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
			int height) {
		try {
			// �����������ͷ�Ĺ�����CameraManager
			CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
			// ���ĳ������ͷ��������֧�ֵĲ���
			CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(mCameraID);
			// ֧�ֵ�STREAM CONFIGURATION
			StreamConfigurationMap map = characteristics
					.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			// ����ͷ֧�ֵ�Ԥ��Size����
			mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
			// �����
			cameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, mHandler);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
			int height) {

	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		return false;
	}

	// �������Ҫע��һ�£���Ϊÿ��һ֡���棬����ص�һ�δ˷���
	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {

	}

	private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

		@Override
		public void onOpened(CameraDevice camera) {
			try {
				mCamera = camera;
				startPreview(mCamera);
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onDisconnected(CameraDevice camera) {

		}

		@Override
		public void onError(CameraDevice camera, int error) {

		}
	};

	// ��ʼԤ������Ҫ��camera.createCaptureSession��δ������Ҫ�������Ự
	private void startPreview(CameraDevice camera) throws CameraAccessException {
		SurfaceTexture texture = mPreviewView.getSurfaceTexture();

		// �������õľ���Ԥ����С
		texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
		Surface surface = new Surface(texture);
		try {
			// ���ò�������ΪԤ�������ﻹ�����հ���¼���
			mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}

		// ���������ͨ�����set(key,value)�����������عⰡ���Զ��۽��Ȳ������� ���¾�����
		// mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

		mImageReader = ImageReader.newInstance(mImageWidth, mImageHeight,
				ImageFormat.JPEG/* �˴����кܶ��ʽ�����������õ�YUV�� */, 2/*
															 * ����ͼƬ����
															 * mImageReader���ܻ�ȡ��ͼƬ��
															 * ��
															 * ����ʵ������2+1��ͼƬ�����Ƕ�һ��
															 */);

		mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,mHandler);
		// ����һ���ֱ�add����surface��һ��Textureview�ģ�һ��ImageReader�ģ����ûadd�������û����ͷԤ��������û��ImageReader���Ǹ��ص�����
		mPreviewBuilder.addTarget(surface);
		mPreviewBuilder.addTarget(mImageReader.getSurface());
		camera.createCaptureSession(
				Arrays.asList(surface, mImageReader.getSurface()),
				mSessionStateCallback, mHandler);
	}

	private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {

		@Override
		public void onConfigured(CameraCaptureSession session) {
			try {
				updatePreview(session);
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onConfigureFailed(CameraCaptureSession session) {

		}
	};

	private void updatePreview(CameraCaptureSession session)
			throws CameraAccessException {
		session.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
	}

	private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

		/**
		 * ����һ��ͼƬ����ʱ��ص��˷���������һ��һ��Ҫע�⣺ һ��Ҫ����
		 * reader.acquireNextImage()��close()������������ͻῨס�����������ұ�����ӿ��˺þã�����
		 * �ܶ��˿���дDemo���������һ��Log�������ס�ˣ����߷�������һֱ���ص���
		 **/
		@Override
		public void onImageAvailable(ImageReader reader) {
			Image img = reader.acquireNextImage();
			/**
			 * ��ΪCamera2��û��Camera1��Priview�ص����������Ը���ô�ܵ�Ԥ��ͼ���byte[]�أ������������ˣ�����
			 * �����˺þõİ취������
			 **/
			ByteBuffer buffer = img.getPlanes()[0].getBuffer();
			// �������ͼƬ��byte������
			byte[] bytes = new byte[buffer.remaining()];
			
			Log.i("huangzheng", "��ȡ��ͼƬ��...   ͼƬ��С��" + bytes.length);
			
			img.close();
		}
	};

	
	protected void onPause() {
		if (null != mCamera) {
			mCamera.close();
			mCamera = null;
		}
		if (null != mImageReader) {
			mImageReader.close();
			mImageReader = null;
		}
		super.onPause();
	}
}