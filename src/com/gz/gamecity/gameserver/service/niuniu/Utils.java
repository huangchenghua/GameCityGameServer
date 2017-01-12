package com.gz.gamecity.gameserver.service.niuniu;

import java.util.Random;

public final class Utils {
	private static Random rd = new Random(System.currentTimeMillis());
	
	
	/**
	 * 获取随机数
	 * @return 大于0的随机整数
	 */
	public static int getRandomInt(){
		return (rd.nextInt() >>> 1);
	}
	
	/**
	 * 获取0~max以内的随机整数
	 * @param max 最大值（不含）
	 * @return 随机整数
	 */
	public static int getRandomInt(int max){
		return getRandomInt() % max;
	}
	
	/**
	 * 获取一个范围内的随机数
	 * @param min 下限（含）
	 * @param max 上限（不含）
	 * @return 随机整数
	 */
	public static int getRandomInt(int min, int max){
		return min + getRandomInt(max - min);
	}
	
	
	/**
	 * 打印数组
	 * @param ary 
	 * @param numPerLine 每行打印多少个数
	 */
	public static String printArray(int[] ary, int numPerLine){
		StringBuffer sb=new StringBuffer("");
		if(numPerLine <= 0) numPerLine = 1;
		if(null == ary){
			System.out.println("［printArray］array is null.");
			return null;
		}
		 
		int aryLen = ary.length;
		System.out.println("［printArray］array length: " + aryLen);
		for(int i = 0; i < aryLen;){
			System.out.print(ary[i] + "\t");
			sb.append(ary[i] + "\t");
			if(++i % numPerLine == 0)System.out.println();
		}
		System.out.println();
		return sb.toString();
	}
	
	/**
	 * 从less~more连续数中随机出一些数（各不相同）组成数组（不适合数组很长的情况）
	 * @param less 下限值 >= 0
	 * @param more 上限值 > 0
	 * @param count 取多少个
	 * @return 数组
	 */
	public static int[] getRandomIntArrayDistinct(int less, int more, int count){
		
		int targetCount = more - less;//可选长度
		
		if(less < 0 || more <= less || count <= 0 || targetCount < count)
			return null;
		
		
		int i = 0;
		int[] aryIdx = new int[targetCount];
		for(;i < targetCount; i++){
			aryIdx[i] = i + less;
		}
		
		if(count == targetCount) 
			return aryIdx;
		
		int[] result = new int[count];
		int randIdx, tempIdx; 
		if(count < targetCount >> 1){//选出随机的
			for(i = 0; i < count; i++){
				randIdx = getRandomInt(i, targetCount);
				tempIdx = aryIdx[i];
				aryIdx[i] = aryIdx[randIdx];
				aryIdx[randIdx] = tempIdx;
			}
			System.arraycopy(aryIdx, 0, result, 0, count);
		} else {//排除随机的
			int excludeCount = targetCount - count;
			for(i = 0; i < excludeCount; i++){
				randIdx = getRandomInt(i, targetCount);
				tempIdx = aryIdx[i];
				aryIdx[i] = aryIdx[randIdx];
				aryIdx[randIdx] = tempIdx;
			}
			System.arraycopy(aryIdx, excludeCount, result, 0, count);
		}
		
		aryIdx = null;
		
		return result;
	}
}
